package nl.absolutevalue.blackbox.storage.oc

import cats.data.Kleisli
import cats.effect.{IO, IOApp, Resource}
import com.github.sardine.{DavResource, Sardine}
import com.typesafe.config.Config
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}

/** Goal of this experiment:
  *   - Make connection to OwnCloud container;
  *   - Use clean configuration without hardcoding anything;
  *   - Map Sardine path to dataset-level paths;
  *   - Prototype flow model with dependency injection and separated IO
  */
object ListingDemo extends IOApp.Simple {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
  val run: IO[Unit] = {

    val config = OwncloudConf.loadF[IO]

    type Deps = (Sardine, OwncloudConf)

    val filesListing = OwncloudFiles.listTopLevel[IO]("/").
      tapWith { case ((_, wdConf), drr) =>
        drr.map { dr =>
          WebdavPath(
            wdConf.serverUri,
            wdConf.serverSuffix,
            Some(dr.getPath.replaceFirst(wdConf.serverSuffix, ""))
          )
        }
      }

    val dataRes = for {
      _ <- Kleisli.ask[Resource[IO, *], OwncloudConf]
      sardine <- Webdav.makeSardine[IO]
      res <- filesListing
        .mapF(Resource.eval)
        .local[OwncloudConf](conf => (sardine, conf.webdavBase))
    } yield res

    val viewIo = (data: List[WebdavPath]) =>
      data.map(n => Logger[IO].info(n.userPath.getOrElse("/"))).reduce(_ >> _)

    Logger[IO].info(s"+ Hello Demo") >>
      config.flatMap(c => dataRes.run(c).use(viewIo)) >>
      Logger[IO].info(s"- End Demo")
  }
}
