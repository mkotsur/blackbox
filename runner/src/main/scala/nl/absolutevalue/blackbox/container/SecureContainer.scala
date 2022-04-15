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
import com.github.dockerjava.api.command.{CreateContainerCmd, InspectContainerResponse}
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import nl.absolutevalue.blackbox.container.SecureContainer.{Artefact, Command, Data, Output}
import nl.absolutevalue.blackbox.docker.DockerContainer
import nl.absolutevalue.blackbox.docker.DockerContainer.CommandsExtensions.*
import nl.absolutevalue.blackbox.docker.DockerContainer.State
import nl.absolutevalue.blackbox.runner.RunnerConf.RemoteFolders
import org.typelevel.log4cats.Logger

import java.io.Closeable
import java.net.URI
import java.nio.file.{Path, Paths}
import scala.collection.View.FlatMap
import scala.concurrent.Future
import scala.util.Failure

object SecureContainer:
  trait Artefact
  case class Command(command: List[String], image: String) extends Artefact

  // Local Home = path on the machine that runs this code.
  case class Script(localHome: Path, runFile: String, runWith: Command) extends Artefact

  // Local Path = path on the machine that runs this code.
  case class Data(localPath: Path)

  // Local Path = path on the machine that runs this code.
  case class Output(localPath: Path)

class SecureContainer[F[_]: Monad: Async: Logger: Applicative](
    dockerUri: URI,
    remoteFolders: RemoteFolders
) {

  import DockerContainer.State
  import DockerContainer.State.*

  import scala.jdk.CollectionConverters.*

  private val logger = Logger[F]

  private val httpClient =
    new ZerodepDockerHttpClient.Builder().dockerHost(dockerUri).build()

  private val dockerClient: DockerClient =
    DockerClientBuilder
      .getInstance()
      .withDockerHttpClient(httpClient)
      .build()

  def create(
      script: Artefact,
      outputOpt: Option[Output] = None,
      dataOpt: Option[Data] = None
  ): F[DockerContainer] = {
    import SecureContainer.{Script, Command}

    val (image, command, scriptBindOpt) = script match {
      case Script(
            localHome,
            runFile,
            Command(executable, image)
          ) =>
        (
          image,
          executable :+ remoteFolders.code.resolve(runFile).toString,
          new Bind(localHome.toString, Volume(remoteFolders.code.toString), AccessMode.ro).some
        )
      case Command(command, image) => (image, command, None)
    }

    val dataBinds = dataOpt
      .map(data =>
        new Bind(data.localPath.toString, Volume(remoteFolders.data.toString), AccessMode.ro)
      )
      .toList

    val outBind = outputOpt.map(out =>
      new Bind(out.localPath.toString, Volume(remoteFolders.output.toString), AccessMode.rw)
    )

    val createContainerCmd = dockerClient
      .createContainerCmd(image)
      .withHostConfig(
        HostConfig
          .newHostConfig()
          .withBinds((dataBinds ++ scriptBindOpt.toList ++ outBind.toList).asJava)
      )
      .withCmd(command.asJava)
      .withNetworkDisabled(true)
      .withAttachStdin(true)
      .withAttachStderr(true)

    for {
      _ <- logger.debug(s"Executing command $script")
      response <- Sync[F].blocking(createContainerCmd.exec())
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

  def run(
      script: Artefact,
      outputOpt: Option[Output] = None,
      dataOpt: Option[Data] = None
  ): Resource[F, (fs2.Stream[F, State], fs2.Stream[F, String])] = {
    val containerF = for {
      secureDockerCnt <- create(script, outputOpt = outputOpt, dataOpt = dataOpt)
      _ <- Sync[F].blocking(dockerClient.startContainerCmd(secureDockerCnt.containerId).exec())
      _ <- logger.debug(s"Container started ${secureDockerCnt.containerId}")
    } yield DockerContainer(secureDockerCnt.containerId)

    Resource
      .make(containerF)(destroy)
      .map(c => (statusStream(c), outputStream(c)))
  }

}
