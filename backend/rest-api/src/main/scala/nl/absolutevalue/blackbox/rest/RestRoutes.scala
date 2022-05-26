package nl.absolutevalue.blackbox.rest
import cats.effect.*
import cats.effect.implicits.*
import cats.effect.std.Queue
import cats.effect.syntax.*
import cats.implicits.*
import cats.{Monad, MonadThrow}
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceSensitiveDataEntityDecoder.circeEntityDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.staticcontent.*

import java.nio.file.{Files, Path}
import java.util.UUID

class RestRoutes[F[_]: Monad: MonadThrow: Async](
    q: Queue[F, (UUID, RunRequest)],
    `ref`: Ref[F, List[RunCompletedResponse]],
    outputsPath: Path
) extends Http4sDsl[F] {

  private val v1: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "submit" =>
      for {
        rr <- req.as[RunRequest]
        reqUUID <- Sync[F].delay(UUID.randomUUID)
        _ <- q.offer((reqUUID, rr))
        res <- Ok(AcceptedResponse(reqUUID.toString))
      } yield res

    case GET -> Root / "completed" =>
      for {
        res <- Ok(ref.get)
      } yield res
  }

  private val outputs: HttpRoutes[F] = fileService(FileService.Config(outputsPath.toString()))

  val all: HttpRoutes[F] = Router(
    "outputs" -> outputs,
    "" -> v1
  )
}
