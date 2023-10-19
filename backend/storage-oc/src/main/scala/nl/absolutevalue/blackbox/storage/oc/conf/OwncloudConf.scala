package nl.absolutevalue.blackbox.storage.oc.conf

import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.derivation.default.*
import pureconfig.module.cats.syntax.*
import cats.effect.Sync

import java.net.URI
import scala.concurrent.duration.FiniteDuration

case class OwncloudConf(
    webdavUsername: String,
    webdavPassword: String,
    sharesSource: String,
    maxFolderDepth: Short,
    webdavBase: WebdavBase
) derives ConfigReader

object OwncloudConf {

  case class WebdavBase(serverUri: URI, serverSuffix: String) derives ConfigReader

  case class ClientConf(
      idleTimeout: FiniteDuration,
      requestTimeout: FiniteDuration,
      connectionTimeout: FiniteDuration,
      responseHeaderTimeout: FiniteDuration
  ) derives ConfigReader

  def configSrc = ConfigSource.default.at("owncloud")

  def loadF[F[_]: Sync]: F[OwncloudConf] = {
    val res = configSrc.load[OwncloudConf].left.map(ff => new RuntimeException(ff.prettyPrint()))
    Sync[F].fromEither[OwncloudConf](res)
  }

}
