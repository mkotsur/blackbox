package nl.absolutevalue.blackbox

import cats.effect.{IO, IOApp}
import nl.absolutevalue.blackbox.docker.DockerContainer
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  private val commandHelloWorld =
    List("python", "-c", "import time; time.sleep(4); print('Hello World!'); time.sleep(4);")

  val run = {
    val container = new SecureContainer[IO]
    val script = SecureContainer.Command(commandHelloWorld, "python:3-alpine")

    container
      .run(script)
      .use { case (stateStream, outputStream) =>
        stateStream
          .compile[IO, IO, DockerContainer.State]
          .toList
          .flatMap(states => logger.info("Last state " + states.last.toString)) >> logger
          .info(">>>") >> outputStream.compile[IO, IO, String].toList
      }
      .flatMap(logs => logger.info(logs.mkString("\n")))
  }

}
