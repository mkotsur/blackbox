package nl.absolutevalue.blackbox.flow.gh.util

import cats.effect.implicits.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.FiniteDuration

object Scheduler {

//  def start[F[_]: Logger: ContextShift: Trace: Concurrent](
//      label: String,
//      interval: FiniteDuration
//  )(thunk: => F[_]): F[Fiber[F, Unit]] =
//    stream[F, Unit](label, interval)(thunk >> Sync[F].unit).compile.drain.start

  def stream[F[_]: Logger: Concurrent: Temporal, O](
      label: String,
      interval: FiniteDuration
  )(thunk: => F[O]): fs2.Stream[F, O] =
    fs2.Stream.eval(Logger[F].debug(s"⏰ Starting $label with interval $interval")) >>
      fs2.Stream
        .awakeEvery[F](interval)
        .evalTap(_ => Logger[F].debug(s"Started $label round"))
        .evalMap(_ => thunk)
        .evalTap(c => Logger[F].debug(s"Completed $label round with $c"))

}
