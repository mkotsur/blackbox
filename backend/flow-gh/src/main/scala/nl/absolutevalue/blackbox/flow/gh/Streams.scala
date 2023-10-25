package nl.absolutevalue.blackbox.flow.gh

import cats.Applicative
import cats.effect.{Concurrent, Temporal}
import nl.absolutevalue.blackbox.flow.gh.util.Scheduler
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

object Streams {

  case class State(name: String)

  def pipelines[F[_]: Logger: Concurrent: Temporal]: fs2.Stream[F, State] = {
    Scheduler.stream("State update", 1.second)(Applicative[F].pure(State("test")))
  }
}
