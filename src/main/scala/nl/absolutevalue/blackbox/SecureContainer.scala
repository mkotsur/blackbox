package nl.absolutevalue.blackbox

import cats.{Applicative, Monad}
import cats.effect.{Async, IO, Sync}
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
object SecureContainer:
  trait Artefact
  case class Script(command: List[String], image: String) extends Artefact

class SecureContainer[F[_]: Sync: Logger: Monad: Applicative] {
  import cats.implicits._
  import cats.syntax._
  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import DockerContainer.State
  import DockerContainer.State.*
  import scala.collection.JavaConverters.*

  private val logger = Logger[F]
  private val dockerClient: DockerClient = DockerClientBuilder.getInstance().build()

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
    } yield DockerContainer(response.getId, Created)
  }

  def destroy(container: DockerContainer): F[Unit] = {
    val command = dockerClient.removeContainerCmd(container.containerId)
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

  def run(script: Script): fs2.Stream[F, State] = {

    val containerF = for {
      created <- create(script)
      _ <- Sync[F].blocking(dockerClient.startContainerCmd(created.containerId).exec())
    } yield DockerContainer(created.containerId, State.Created)

    fs2.Stream.eval(containerF).flatMap(statusStream)
  }

}
