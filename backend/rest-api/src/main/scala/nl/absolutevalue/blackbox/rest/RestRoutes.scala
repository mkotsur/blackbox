package nl.absolutevalue.blackbox.rest
import cats.effect.*
import cats.effect.implicits.*
import cats.effect.std.Queue
import cats.effect.syntax.*
import cats.implicits.*
import cats.{Monad, MonadThrow}
import io.circe.generic.auto.*
import nl.absolutevalue.blackbox.datasets.Datasets
import nl.absolutevalue.blackbox.runner.RunnerConf
import nl.absolutevalue.blackbox.runner.RunnerConf.RemoteFolders
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceSensitiveDataEntityDecoder.circeEntityDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.staticcontent.*
import scala.collection.immutable.Seq

import java.nio.file.Path
import java.util.UUID
import RestRoutes.*
import fs2.io.file.Files
import io.circe.{Encoder, Json}

import scala.collection.immutable.Seq

object RestRoutes {
  case class ContainerInfo(
      mountFolders: RemoteFolders,
      datasets: Seq[(Path, Option[Long])]
  )
  implicit val encodePath: Encoder[Path] = (a: Path) => Json.fromString(a.toString)
}

class RestRoutes[F[_]: Monad: MonadThrow: Async: Files](
    registerRequest: ((UUID, RunRequest)) => F[Unit],
    completedsRef: Ref[F, List[RunCompletedResponse]],
    runnerConf: RunnerConf,
    datasets: Datasets[F]
) extends Http4sDsl[F] {

  private val v1: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "submit" =>
      for {
        rr <- req.as[RunRequest]
        reqUUID <- Sync[F].delay(UUID.randomUUID)
        _ <- registerRequest((reqUUID, rr))
        res <- Ok(AcceptedResponse(reqUUID.toString))
      } yield res

    case GET -> Root / "completed" =>
      for {
        res <- Ok(completedsRef.get)
      } yield res

    case GET -> Root / "environment" =>
      datasets.list.use(paths =>
        Ok(
          ContainerInfo(
            runnerConf.mountFolders,
            paths
          )
        )
      )
  }


  private val outputs: HttpRoutes[F] = fileService[F](
    FileService.Config[F](runnerConf.outputsPath.toString)
  )

  val all: HttpRoutes[F] = Router(
    "outputs" -> outputs,
    "" -> v1
  )
}
