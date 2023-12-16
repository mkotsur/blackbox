package nl.absolutevalue.blackbox.storage.azure

import cats.effect.{IO, IOApp}
import nl.absolutevalue.blackbox.storage.azure.conf.AzureConf
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}

/** Goal of this experiment:
  *   - Make connection to Azure container;
  *   - Use clean configuration without hard-coding anything;
  *   - Map FTP path to dataset-level paths;
  */
object ListingDemo extends IOApp.Simple {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
  val run: IO[Unit] = {

    val config = AzureConf.loadF[IO]

    type Deps = (AzureConf)

    for {
      conf <- AzureConf.loadF[IO]
      _ <- Logger[IO].info(s"+ Hello Demo")
      listing <- AzureFiles.listTopLevel[IO]("/").run(conf)
      _ <- listing.map(res => Logger[IO].info(res.path)).foldLeft(IO.unit)(_ >> _)
      _ <- Logger[IO].info(s"- End Demo")
    } yield ()
  }
}
