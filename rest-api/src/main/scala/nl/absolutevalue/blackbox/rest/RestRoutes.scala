package nl.absolutevalue.blackbox.rest
import cats.{Monad, MonadThrow}
import cats.effect.{Concurrent, IO, Ref}
import cats.effect.std.Queue
import org.http4s.HttpRoutes
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceSensitiveDataEntityDecoder.circeEntityDecoder
import org.http4s.implicits.*
import org.http4s.circe.*
import io.circe.generic.auto.*
import cats.effect.implicits.*
import cats.implicits.*
import cats.effect.syntax.*
import org.http4s.dsl.Http4sDsl

class RestRoutes[F[_]: Monad: MonadThrow: Concurrent](
    q: Queue[F, RunRequest],
    ref: Ref[F, List[RunCompletedResponse]]
) extends Http4sDsl[F] {

  val all: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "submit" =>
      for {
        rr <- req.as[RunRequest]
        _ <- q.offer(rr)
        res <- Ok(s"Hello, ${rr.language}.")
      } yield res

    case req @ GET -> Root / "completed" =>
      for {
        res <- Ok(ref.get)
      } yield res
  }
}
