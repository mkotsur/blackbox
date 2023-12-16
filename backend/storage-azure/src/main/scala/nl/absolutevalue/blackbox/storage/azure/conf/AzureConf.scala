package nl.absolutevalue.blackbox.storage.azure.conf

import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.derivation.default.*
import pureconfig.module.cats.syntax.*
import cats.effect.Sync

import java.net.URI
import scala.concurrent.duration.FiniteDuration

case class AzureConf(
    ftpUsername: String,
    ftpPassword: String,
    accountName: String,
    containerName: String
) derives ConfigReader

object AzureConf {

  val blobDomain = "blob.core.windows.net"
  val securePort = 22

  def connectionString(accountName: String, containerName: String, username: String) =
    //TODO: container name is optional
    s"$accountName.$containerName.$username@$accountName.$blobDomain"

  case class ClientConf(
      idleTimeout: FiniteDuration,
      requestTimeout: FiniteDuration,
      connectionTimeout: FiniteDuration,
      responseHeaderTimeout: FiniteDuration
  ) derives ConfigReader

  def configSrc = ConfigSource.default.at("azure")

  def loadF[F[_]: Sync]: F[AzureConf] = {
    val res = configSrc.load[AzureConf].left.map(ff => new RuntimeException(ff.prettyPrint()))
    Sync[F].fromEither[AzureConf](res)
  }

}
