package nl.absolutevalue.blackbox.flow.drone.conf

import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.derivation.default.*
import pureconfig.module.cats.syntax.*
import cats.effect.Sync

import java.net.URI
import scala.concurrent.duration.FiniteDuration

case class DroneConf(
    token: String
) derives ConfigReader

object OwncloudConf {

  def configSrc = ConfigSource.default.at("flow.drone")

  def loadF[F[_]: Sync]: F[DroneConf] = {

    //TODO: is this really a good way?
    val res = configSrc.load[DroneConf].left.map(ff => new RuntimeException(ff.prettyPrint()))
    Sync[F].fromEither[DroneConf](res)
  }

}
