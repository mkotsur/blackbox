package nl.absolutevalue.blackbox

import cats.{Applicative, Monad}
import cats.effect.{Async, Concurrent, IO, Sync, SyncIO, unsafe}
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder
import nl.absolutevalue.blackbox.SecureContainer.Script
import org.typelevel.log4cats.Logger
import nl.absolutevalue.blackbox.docker.DockerContainer.State

import scala.collection.View.FlatMap
import scala.concurrent.Future
import cats.effect.kernel.Resource.ExitCase
import cats.data.EitherT
import com.github.dockerjava.api.command.InspectContainerResponse
import nl.absolutevalue.blackbox.docker.DockerContainer
import cats.effect.kernel.{Deferred, Resource, Spawn}
import com.github.dockerjava.api.model.Frame
import cats.implicits.*

import scala.util.Failure
import cats.MonadError
import cats.effect.std.{Dispatcher, Queue}
import com.github.dockerjava.api.async.ResultCallbackTemplate
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import cats.syntax.*

import java.io.Closeable
import nl.absolutevalue.blackbox.docker.DockerContainer.CommandsExtensions.*

object SecureContainer:
  trait Artefact
  case class Script(command: List[String], image: String) extends Artefact

class SecureContainer[F[_]: Monad: Async: Logger: Applicative] {

  import DockerContainer.State
  import DockerContainer.State.*
  import scala.jdk.CollectionConverters.*

  private val logger = Logger[F]
//
//  private val client =
//    new ZerodepDockerHttpClient.Builder().dockerHost(new URI("localhost")).build()
  private val dockerClient: DockerClient =
    DockerClientBuilder
      .getInstance()
      .build()

  def create(script: Script): F[DockerContainer] = {
    val command = dockerClient
      .createContainerCmd(script.image)
      .withNetworkDisabled(true)
      //          .withHostConfig(containerEnv.hostConfig)
      .withCmd(script.command.asJava)
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

  def outputStream(c: DockerContainer): fs2.Stream[F, String] = {

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

  def runR(script: Script): Resource[F, (fs2.Stream[F, State], fs2.Stream[F, String])] = {
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
