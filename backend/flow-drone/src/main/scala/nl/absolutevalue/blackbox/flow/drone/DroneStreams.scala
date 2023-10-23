package nl.absolutevalue.blackbox.flow.drone

import cats.Applicative
import cats.effect.{Concurrent, Temporal}
import nl.absolutevalue.blackbox.flow.drone.util.Scheduler
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

object DroneStreams {

  case class DroneState(name: String)

  def pipelines[F[_]: Logger: Concurrent: Temporal]: fs2.Stream[F, DroneState] = {
    Scheduler.stream("State update", 1.second)(Applicative[F].pure(DroneState("test")))
  }
}
