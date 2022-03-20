package nl.absolutevalue.blackbox

import cats.{Applicative, Monad}
import cats.effect.{Async, Concurrent, IO, Sync, SyncIO, unsafe}
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder
import nl.absolutevalue.blackbox.SecureContainer.{Artefact, Command}
import org.typelevel.log4cats.Logger
import nl.absolutevalue.blackbox.docker.DockerContainer.State

import scala.collection.View.FlatMap
import scala.concurrent.Future
import cats.effect.kernel.Resource.ExitCase
import cats.data.EitherT
import com.github.dockerjava.api.command.InspectContainerResponse
import nl.absolutevalue.blackbox.docker.DockerContainer
import cats.effect.kernel.{Deferred, Resource, Spawn}
import com.github.dockerjava.api.model.{AccessMode, Bind, Frame, HostConfig, Volume}
import cats.implicits.*

import scala.util.Failure
import cats.MonadError
import cats.effect.std.{Dispatcher, Queue}
import com.github.dockerjava.api.async.ResultCallbackTemplate
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import cats.syntax.*

import java.io.Closeable
import nl.absolutevalue.blackbox.docker.DockerContainer.CommandsExtensions.*

import java.net.URI
import java.nio.file.Path

object SecureContainer:
  trait Artefact
  case class Command(command: List[String], image: String) extends Artefact

  // Local Home = path on the machine that runs this code.
  case class PythonScript(localHome: Path, runFile: String, image: String) extends Artefact

class SecureContainer[F[_]: Monad: Async: Logger: Applicative] {

  import DockerContainer.State
  import DockerContainer.State.*
  import scala.jdk.CollectionConverters.*

  private val logger = Logger[F]

  private val MountFolder = "/tmp/script"

  private val httpClient =
    new ZerodepDockerHttpClient.Builder().dockerHost(new URI("unix:///var/run/docker.sock")).build()

  private val dockerClient: DockerClient =
    DockerClientBuilder
      .getInstance()
      .withDockerHttpClient(httpClient)
      .build()

  def create(script: Artefact): F[DockerContainer] = {
    val commandPart = script match {
      case SecureContainer.PythonScript(localHome, runFile, image) =>
        val bind = new Bind(localHome.toString, Volume(MountFolder), AccessMode.ro)
        dockerClient
          .createContainerCmd(image)
          .withHostConfig(HostConfig.newHostConfig().withBinds(bind))
          .withCmd(List("python", s"${MountFolder}/$runFile").asJava)
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
      res <- DockerContainer.State.state(state.getStatus, state.getExitCode) match
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
