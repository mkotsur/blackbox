package nl.absolutevalue.blackbox.docker

import cats.effect.kernel.Spawn
import cats.{Applicative, Monad}
import cats.effect.{Async, Sync}
import cats.effect.std.Dispatcher
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.LogContainerCmd
import com.github.dockerjava.api.model.Frame
import org.typelevel.log4cats.Logger

import java.io.Closeable

case class DockerContainer(containerId: String)

object DockerContainer {
  enum State(terminal: Boolean):
    case Created extends State(false)
    case Running extends State(false)
    case ExitFail(code: Int) extends State(terminal = true)
    case ExitSuccess extends State(terminal = true)

  object State {

    class StateParsingError(state: String, code: Int)
        extends RuntimeException(
          s"Couldn't parse Docker container state \"$state\" with code \"$code\""
        )

    def state(state: String, code: Int): Either[StateParsingError, State] = {
      state.toLowerCase match {
        case "running"             => Right(Running)
        case "exited" if code == 0 => Right(ExitSuccess)
        case "exited"              => Right(ExitFail(code))
        case _                     => Left(new StateParsingError(state, code))
      }
    }
  }

  object CommandsExtensions {

    extension (command: LogContainerCmd) {
      def execF[F[_]: Monad: Logger](
          dispatcher: Dispatcher[F],
          q: cats.effect.std.Queue[F, Option[String]]
      ): ResultCallback.Adapter[Frame] = {
        import cats.effect.syntax.*
        import cats.implicits.*
        command
          .exec(
            new com.github.dockerjava.api.async.ResultCallback.Adapter[Frame] {

              override def onNext(item: Frame): Unit = {
                super.onNext(item)
                val line = new String(item.getPayload)
                dispatcher.unsafeRunSync(q.offer(line.some))
              }

              override def onComplete(): Unit = {
                super.onComplete()
                dispatcher.unsafeRunAndForget(q.offer(None))
              }

              override def onError(throwable: Throwable): Unit = {
                super.onError(throwable)
                dispatcher.unsafeRunSync(Logger[F].error(throwable)("Error during logs streaming"))
              }
            }
          )
          .awaitCompletion()
      }
    }
  }
}
