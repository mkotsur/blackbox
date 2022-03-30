package nl.absolutevalue.blackbox.container

import cats.data.EitherT
import cats.effect.kernel.Resource.ExitCase
import cats.effect.kernel.{Deferred, Resource, Spawn}
import cats.effect.std.{Dispatcher, Queue}
import cats.effect.*
import cats.implicits.*
import cats.syntax.*
import cats.{Applicative, Monad, MonadError}
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallbackTemplate
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import nl.absolutevalue.blackbox.container.SecureContainer.{Artefact, Command}
import nl.absolutevalue.blackbox.docker.DockerContainer
import nl.absolutevalue.blackbox.docker.DockerContainer.CommandsExtensions.*
import nl.absolutevalue.blackbox.docker.DockerContainer.State
import org.typelevel.log4cats.Logger

import java.io.Closeable
import java.net.URI
import java.nio.file.Path
import scala.collection.View.FlatMap
import scala.concurrent.Future
import scala.util.Failure

object SecureContainer:
  trait Artefact
  case class Command(command: List[String], image: String) extends Artefact

  // Local Home = path on the machine that runs this code.
  case class Script(localHome: Path, runFile: String, runWith: Command) extends Artefact

class SecureContainer[F[_]: Monad: Async: Logger: Applicative] {

  import DockerContainer.State
  import DockerContainer.State.*

  import scala.jdk.CollectionConverters.*

  private val logger = Logger[F]

  //TODO: extract in conf
  private val MountFolder = "/tmp/script"

  //TODO: extract in conf
  private val httpClient =
    new ZerodepDockerHttpClient.Builder().dockerHost(new URI("unix:///var/run/docker.sock")).build()

  private val dockerClient: DockerClient =
    DockerClientBuilder
      .getInstance()
      .withDockerHttpClient(httpClient)
      .build()

  def create(script: Artefact): F[DockerContainer] = {
    import SecureContainer.{Script, Command}
    val commandPart = script match {
      case Script(
            localHome,
            runFile,
            Command(executable, image)
          ) =>
        val bind = new Bind(localHome.toString, Volume(MountFolder), AccessMode.ro)
        dockerClient
          .createContainerCmd(image)
          .withHostConfig(HostConfig.newHostConfig().withBinds(bind))
          .withCmd((executable :+ s"$MountFolder/$runFile").asJava)
      case SecureContainer.Command(command, image) =>
        dockerClient
          .createContainerCmd(image)
          .withCmd(command.asJava)
    }

    val command = commandPart
      .withNetworkDisabled(true)
      .withAttachStdin(true)
      .withAttachStderr(true)

    for {
      _ <- logger.debug(s"Executing command $script")
      response <- Sync[F].blocking(command.exec())
      _ <- logger.info(s"Created Docker container ${response.getId}")
    } yield DockerContainer(response.getId)
  }

  def destroy(container: DockerContainer): F[Unit] = {
    val command = dockerClient.removeContainerCmd(container.containerId).withForce(true)
    Sync[F].blocking(command.exec())
  }

  private def state(c: DockerContainer): F[State] = {
    val command = dockerClient.inspectContainerCmd(c.containerId)
    val containerStateF = Sync[F].blocking(command.exec().getState)

    for {
      state <- containerStateF
      res <- DockerContainer.State.state(state.getStatus, state.getExitCodeLong.toInt) match
        case Right(value) => Applicative[F].pure(value)
        case Left(error)  => Sync[F].raiseError(error)
    } yield res

  }

  private def statusStream(
      c: DockerContainer
  ): fs2.Stream[F, State] =
    fs2.Stream
      .eval(state(c))
      .flatMap {
        case Running => fs2.Stream(Running) ++ statusStream(c)
        case s       => fs2.Stream(s)
      }

  private def outputStream(c: DockerContainer): fs2.Stream[F, String] = {

    val command = dockerClient
      .logContainerCmd(c.containerId)
      .withFollowStream(true)
      .withTailAll()
      .withStdErr(true)
      .withStdOut(true)
      .withTimestamps(false)

    for {
      dispatcher <- fs2.Stream.resource(Dispatcher[F])
      q <- fs2.Stream.eval(Queue.unbounded[F, Option[String]])
      _ <- fs2.Stream.eval(
        Async[F].delay(
          command.execF[F](dispatcher, q)
        )
      )
      line <- fs2.Stream.fromQueueNoneTerminated(q)
    } yield line
  }

  def run(script: Artefact): Resource[F, (fs2.Stream[F, State], fs2.Stream[F, String])] = {
    val containerF = for {
      secureDockerCnt <- create(script)
      _ <- Sync[F].blocking(dockerClient.startContainerCmd(secureDockerCnt.containerId).exec())
      _ <- logger.debug(s"Container started ${secureDockerCnt.containerId}")
    } yield DockerContainer(secureDockerCnt.containerId)

    Resource
      .make(containerF)(destroy)
      .map(c => (statusStream(c), outputStream(c)))
  }

}