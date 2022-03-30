package nl.absolutevalue.blackbox.rest
import cats.effect.kernel.{Async, Spawn}
import cats.{Applicative, Monad, MonadError, ~>}
import cats.effect.{Async, FiberIO, IO, MonadCancel, Ref, Resource, Sync}
import cats.effect.std.Queue
import fs2.Pipe
import nl.absolutevalue.blackbox.docker.DockerContainer
import nl.absolutevalue.blackbox.container.SecureContainer

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import cats.implicits.*
import cats.effect.implicits.*
import nl.absolutevalue.blackbox.util.TempFiles
import org.typelevel.log4cats.Logger

import java.util.UUID

class RestContainerDispatcher[F[_]: Async: Logger](
    responsesRef: Ref[F, List[AcceptedResponse]],
    completedsRef: Ref[F, List[RunCompletedResponse]],
    sc: SecureContainer[F]
) {

  def dispatch: RunRequest => F[AcceptedResponse] = rr => {

    def secureContainerWithScriptRes(re: SecureContainer.Command) = for {
      tempDir <- TempFiles.tempDir[F]
      _ <- Resource.eval(Logger[F].debug(s"Create temp directory ${tempDir.toString}"))
      _ <- Resource.eval(
        Sync[F].delay(
          Files.write(tempDir.resolve("script.bb"), rr.code.getBytes(StandardCharsets.UTF_8))
        )
      )
      res <- sc.run(SecureContainer.Script(tempDir, "script.bb", re))
    } yield res

    for {
      requestId <- Sync[F].delay(UUID.randomUUID.toString)
      _ <- Logger[F].debug(s"Assigning id $requestId to the run request $rr")
      runWith <- rr.language match {
        case "python" => SecureContainer.Command(List("python"), "python:3-alpine").pure[F]
        case "r"      => SecureContainer.Command(List("Rscript"), "rocker/r-base:4.1.3").pure[F]
        case other =>
          Sync[F].raiseError(new RuntimeException(s"Language $other is not supported"))
      }
      acceptedResponse <- for {
        f <- secureContainerWithScriptRes(runWith).use { case (statesStream, outStream) =>
          for {
            outs <- outStream.compile.toList
            _ <- Logger[F].debug(s"Read ${outs.size} lines of code")
            _ <- completedsRef.update(
              _ :+ RunCompletedResponse(0, outs.mkString("\n"), "")
            )
          } yield ()
        }.start
      } yield AcceptedResponse(requestId)
    } yield acceptedResponse
  }

}
