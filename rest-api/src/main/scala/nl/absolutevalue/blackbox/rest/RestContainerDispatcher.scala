package nl.absolutevalue.blackbox.rest
import cats.effect.kernel.{Async, Spawn}
import cats.{Applicative, Monad, MonadError, ~>}
import cats.effect.{Async, FiberIO, IO, MonadCancel, Ref, Resource, Sync}
import cats.effect.std.Queue
import fs2.Pipe
import nl.absolutevalue.blackbox.docker.DockerContainer
import nl.absolutevalue.blackbox.container.SecureContainer

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import cats.implicits.*
import cats.effect.implicits.*
import nl.absolutevalue.blackbox.container.SecureContainer.Data
import nl.absolutevalue.blackbox.util.TempFiles
import org.typelevel.log4cats.Logger

import java.time.LocalDateTime
import java.util.UUID

class RestContainerDispatcher[F[_]: Async: Logger](
    responsesRef: Ref[F, List[AcceptedResponse]],
    completedsRef: Ref[F, List[RunCompletedResponse]],
    sc: SecureContainer[F]
) {

  def dispatch: ((UUID, RunRequest)) => F[UUID] = { case (requestUUID, rr) =>
    def secureContainerWithScriptRes(re: SecureContainer.Command) = for {
      tempDir <- TempFiles.tempDir[F]
      _ <- Resource.eval(Logger[F].debug(s"Create temp directory ${tempDir.toString}"))
      _ <- Resource.eval(
        Sync[F].delay(
          Files.write(tempDir.resolve("script.bb"), rr.code.getBytes(StandardCharsets.UTF_8))
        )
      )
      dataPath <- Resource.eval(Sync[F].delay(Path.of(getClass.getResource("/data/").toURI)))
      res <- sc.run(SecureContainer.Script(tempDir, "script.bb", re), Data(dataPath).some)
    } yield res

    for {
      _ <- Logger[F].debug(s"Processing id ${requestUUID.toString} to the run request $rr")
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
            statesStream <- statesStream.compile.toList
            exitCode = statesStream.last match {
              case DockerContainer.State.ExitFail(code) => code.some
              case DockerContainer.State.ExitSuccess    => 0.some
              case _                                    => None
            }
            _ <- Logger[F].debug(s"Read ${outs.size} lines of code")
            now <- Sync[F].delay(LocalDateTime.now())
            _ <- completedsRef.update(
              _ :+ RunCompletedResponse(
                requestUUID.toString,
                exitCode,
                outs.mkString("\n"),
                "",
                now,
                rr
              )
            )
          } yield ()
        }.start
      } yield ()
    } yield requestUUID
  }

}
