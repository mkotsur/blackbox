package nl.absolutevalue.blackbox.storage.azure

import cats.data.Kleisli
import cats.effect.{Async, Sync}
import com.github.sardine.{DavResource, Sardine}
import fs2.ftp.FtpSettings.{FtpCredentials, SecureFtpSettings}
import fs2.ftp.{FtpResource, SecureFtp}
import nl.absolutevalue.blackbox.storage.azure.conf.AzureConf

import scala.jdk.CollectionConverters.*
object AzureFiles {

  //  TODO: SardineException 502 => retry
  //  TODO: org.apache.http.NoHttpResponseException => retry
  //  TODO: javax.net.ssl.SSLException: Connection reset
  def listTopLevel[F[_]: Async](
      userPath: String
  ): Kleisli[F, (AzureConf), List[FtpResource]] = {
    Kleisli { case (azureConf: AzureConf) =>
      val settings = SecureFtpSettings(
        host = s"${azureConf.accountName}.${AzureConf.blobDomain}",
        port = 22,
        credentials = FtpCredentials(
          s"${azureConf.accountName}.${azureConf.containerName}.${azureConf.ftpUsername}",
          azureConf.ftpPassword
        )
      )

      SecureFtp
        .connect[F](settings)
        .use(
          _.ls("/").compile.toList
        )
    }
  }
}
