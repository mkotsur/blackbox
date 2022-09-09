package nl.absolutevalue.blackbox.rest

import cats.effect.std.Queue
import cats.effect.{IO, IOApp, Ref, Resource, Sync}
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io.*
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceSensitiveDataEntityDecoder.circeEntityDecoder
import org.http4s.implicits.*
import org.http4s.circe.*
import io.circe.generic.auto.*
import org.http4s.dsl.io.*
import cats.implicits.*
import cats.syntax.*
import nl.absolutevalue.blackbox.container.SecureContainer
import nl.absolutevalue.blackbox.datasets.Datasets
import nl.absolutevalue.blackbox.rest.RestContainerDispatcher
import nl.absolutevalue.blackbox.runner.RunnerConf

import java.util.UUID

object RestApp extends IOApp.Simple {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val run: IO[Nothing] = {

    def blazeServer(routes: HttpRoutes[IO]) = BlazeServerBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(routes.orNotFound)
      .resource
      .use(_ => IO.never)

    for {
      runRequestQ <- Queue.unbounded[IO, (UUID, RunRequest)]
      runRequestS = fs2.Stream.fromQueueUnterminated(runRequestQ)

      acceptedsRef <- Ref[IO].of[List[AcceptedResponse]](Nil)
      completedsRef <- Ref[IO].of[List[RunCompletedResponse]](Nil)
      runnerConf <- RunnerConf.loadF[IO]
      sc = new SecureContainer[IO](runnerConf.dockerUri, runnerConf.mountFolders)
      datasets = new Datasets[IO](runnerConf.dataSamplesPath)

      dsAbsPath <- Sync[IO].delay(
        // Because the default value is relative, normalize it first
        runnerConf.dataSamplesPath.toFile.getCanonicalFile.toPath
      )
      _ <- Logger[IO].info(s"Loading data samples from ${dsAbsPath.toString}")
      dispatcher = new RestContainerDispatcher[IO](
        acceptedsRef,
        completedsRef,
        dsAbsPath,
        runnerConf,
        sc
      )
      never <- (
        blazeServer(
          new RestRoutes[IO](
            registerRequest = runRequestQ.offer,
            completedsRef,
            runnerConf,
            datasets
          ).all
        ),
        runRequestS
          .through(_.parEvalMap(10)(dispatcher.dispatch))
          .handleErrorWith(e => fs2.Stream.eval(logger.error(e)("Error when processing request")))
          .compile
          .drain
      ).parTupled >> IO.never
    } yield never

  }

}
