package nl.absolutevalue.blackbox.flow.drone

import cats.data.Kleisli
import cats.effect.{IO, IOApp, Resource}
import com.github.sardine.{DavResource, Sardine}
import com.typesafe.config.Config
import nl.absolutevalue.blackbox.flow.drone.DroneStreams
import nl.absolutevalue.blackbox.flow.drone.DroneStreams.DroneState
import nl.absolutevalue.blackbox.flow.drone.conf.OwncloudConf
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}

/** Goal of this experiment:
  *   - Make connection to OwnCloud container;
  *   - Use clean configuration without hardcoding anything;
  *   - Map Sardine path to dataset-level paths;
  *   - Prototype flow model with dependency injection and separated IO
  */
object FlowDemo extends IOApp.Simple {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
  val run: IO[Unit] = {

//    val sink : fs2.Pipe[IO, DroneState, Unit] = Files[IO].writeAll(Path(writeTo))
    DroneStreams.pipelines[IO].evalMap(s => logger.info(s.toString)).compile.drain >>
      Logger[IO].info(s"- End Demo")
  }
}
