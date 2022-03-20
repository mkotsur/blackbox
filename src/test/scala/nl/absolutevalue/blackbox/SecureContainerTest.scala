package nl.absolutevalue.blackbox

import cats.effect.kernel.Async
import cats.effect.kernel.Resource.Pure
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, MonadCancel, Resource, Sync, SyncIO, unsafe}
import cats.effect.unsafe.IORuntime
import cats.implicits.*
import cats.syntax.*
import nl.absolutevalue.blackbox.SecureContainer
import org.scalatest.funsuite.{AnyFunSuite, AsyncFunSuite}
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import docker.DockerContainer.State
import nl.absolutevalue.blackbox.docker.DockerContainer
import org.scalatest.Checkpoints

import java.nio.file.Path
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class SecureContainerTest extends AsyncFunSuite with AsyncIOSpec with Matchers:

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override implicit lazy val ioRuntime: IORuntime = cats.effect.unsafe.implicits.global

  test("Should create container") {
    val container = new SecureContainer[IO]
    val script = SecureContainer.Command(List("test"), "python:3.11.0a5-alpine3.15")

    MonadCancel[IO]
      .bracket(container.create(script))(_.pure[IO])(container.destroy)
      .asserting(_.containerId should not be empty)
  }

  test("Should run a script in a container") {
    val container = new SecureContainer[IO]
    val script = SecureContainer.Command(List("echo", "Hello World!"), "python:3-alpine")

    container
      .runR(script)
      .use { case (stateStream, outputStream) => stateStream.compile[IO, IO, State].toList }
      .asserting(states => {
        states.last should matchPattern { case State.ExitSuccess => }
      })

  }

  test("Process output of a quickly terminating container") {
    logger.info(s"Io Runtime ${ioRuntime}")
    val container = new SecureContainer[IO]
    val script = SecureContainer.Command(List("echo", "Hello World!"), "python:3-alpine")

    container
      .runR(script)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(logs => logs.last shouldBe "Hello World!\n")
  }

  test("Capture two lines of output") {
    logger.info(s"Io Runtime ${ioRuntime}")
    val container = new SecureContainer[IO]
    val script = SecureContainer.Command(List("echo", "Hello\nWorld!"), "python:3-alpine")

    container
      .runR(script)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(_ shouldBe List("Hello\n", "World!\n"))
  }

  test("Process delayed output of a container") {
    val container = new SecureContainer[IO]
    val script = SecureContainer.Command(
      List("python", "-c", "import time; time.sleep(3); print('Hello World!');"),
      "python:3-alpine"
    )

    container
      .runR(script)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(logs => logs.last shouldBe "Hello World!\n")
  }

  test("Process quick output of a delayed container") {
    val container = new SecureContainer[IO]
    val script = SecureContainer.Command(
      List("python", "-c", "import time; print('Hello World!'); time.sleep(3);"),
      "python:3-alpine"
    )

    container
      .runR(script)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(logs => logs.last shouldBe "Hello World!\n")
  }

  test("Mount a script as a volume and run it") {
    val container = new SecureContainer[IO]
    val scriptPath = Path.of(getClass.getResource("/test1.py").toURI)
    val script = SecureContainer.PythonScript(
      scriptPath.getParent,
      scriptPath.getFileName.toString,
      "python:3-alpine"
    )

    container
      .runR(script)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(logs => logs.last shouldBe "Hello World From The Script!\n")
  }
