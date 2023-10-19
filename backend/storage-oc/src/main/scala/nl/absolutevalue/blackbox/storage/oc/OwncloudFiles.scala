package nl.absolutevalue.blackbox.storage.oc

import cats.data.Kleisli
import cats.effect.Sync
import com.github.sardine.{DavResource, Sardine}
import scala.jdk.CollectionConverters.*
object OwncloudFiles {

  //  TODO: SardineException 502 => retry
  //  TODO: org.apache.http.NoHttpResponseException => retry
  //  TODO: javax.net.ssl.SSLException: Connection reset
  def listTopLevel[F[_]: Sync](
      userPath: String
  ): Kleisli[F, (Sardine, WebdavBase), List[DavResource]] = {
    Kleisli { case (sardine, WebdavBase(serverUri, serverSuffix)) =>
      import io.lemonlabs.uri.typesafe.dsl.{pathPartToUrlDsl, urlToUrlDsl}
      Sync[F].delay {
        val url = (serverUri.toURL.toExternalForm / serverSuffix / userPath)
          .normalize(removeEmptyPathParts = true)
          .toStringPunycode
        val resources = sardine.list(url, 1)
        resources.asScala.toList.tail // Skip the folder/file itself
      }
    }
  }
}
