package nl.absolutevalue.blackbox.container

import cats.effect.kernel.Async
import cats.effect.kernel.Resource.Pure
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.IORuntime
import cats.effect.*
import cats.implicits.*
import cats.syntax.*
import nl.absolutevalue.blackbox.container.SecureContainer
import nl.absolutevalue.blackbox.container.SecureContainer.Data
import nl.absolutevalue.blackbox.docker.DockerContainer
import nl.absolutevalue.blackbox.docker.DockerContainer.State
import nl.absolutevalue.blackbox.runner.RunnerConf.MountFolders
import org.scalatest.Checkpoints
import org.scalatest.funsuite.{AnyFunSuite, AsyncFunSuite}
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}

import java.net.URI
import java.nio.file.{Path, Paths}
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class SecureContainerTest extends AsyncFunSuite with AsyncIOSpec with Matchers:

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override implicit lazy val ioRuntime: IORuntime = cats.effect.unsafe.implicits.global

  private val pythonRunWith = SecureContainer.Command(List("python"), "python:3-alpine")
  private val mntFolders = MountFolders(Paths.get("/tmp/code"), Paths.get("/tmp/data"))
  private val dockerUri = new URI("unix:///var/run/docker.sock")

  test("Should create container") {

    val container = new SecureContainer[IO](dockerUri, mntFolders)
    val script = SecureContainer.Command(List("test"), "python:3-alpine")

    MonadCancel[IO]
      .bracket(container.create(script))(_.pure[IO])(container.destroy)
      .asserting(_.containerId should not be empty)
  }

  test("Should run a command in a container") {
    val container = new SecureContainer[IO](dockerUri, mntFolders)
    val script = SecureContainer.Command(List("echo", "Hello World!"), "python:3-alpine")

    container
      .run(script)
      .use { case (stateStream, outputStream) => stateStream.compile[IO, IO, State].toList }
      .asserting(states => {
        states.last should matchPattern { case State.ExitSuccess => }
      })

  }

  test("Process single line of output of a quick command in a container") {
    logger.info(s"Io Runtime ${ioRuntime}")
    val container = new SecureContainer[IO](dockerUri, mntFolders)
    val script = SecureContainer.Command(List("echo", "Hello World!"), "python:3-alpine")

    container
      .run(script)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(logs => logs.last shouldBe "Hello World!\n")
  }

  test("Process two lines of output of a quick command in a container") {
    logger.info(s"Io Runtime ${ioRuntime}")
    val container = new SecureContainer[IO](dockerUri, mntFolders)
    val script = SecureContainer.Command(List("echo", "Hello\nWorld!"), "python:3-alpine")

    container
      .run(script)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(_ shouldBe List("Hello\n", "World!\n"))
  }

  test("Process delayed Python output of a container") {
    val container = new SecureContainer[IO](dockerUri, mntFolders)
    val script = SecureContainer.Command(
      List("python", "-c", "import time; time.sleep(3); print('Hello World!');"),
      "python:3-alpine"
    )

    container
      .run(script)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(logs => logs.last shouldBe "Hello World!\n")
  }

  test("Process quick Python output of a delayed container") {
    val container = new SecureContainer[IO](dockerUri, mntFolders)
    val script = SecureContainer.Command(
      List("python", "-c", "import time; print('Hello World!'); time.sleep(3);"),
      "python:3-alpine"
    )

    container
      .run(script)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(logs => logs.last shouldBe "Hello World!\n")
  }

  test("Mount a Python script as a volume and run it") {
    val container = new SecureContainer[IO](dockerUri, mntFolders)
    val scriptPath = Path.of(getClass.getResource("/test1.py").toURI)
    val script = SecureContainer.Script(
      scriptPath.getParent,
      scriptPath.getFileName.toString,
      SecureContainer.Command(List("python"), "python:3-alpine")
    )

    container
      .run(script)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(logs => logs.last shouldBe "Hello World From The Script!\n")
  }

  test("Process quick R output in a container") {
    val container = new SecureContainer[IO](dockerUri, mntFolders)
    val script = SecureContainer.Command(
      List("Rscript", "-e", "print('Hello World!');"),
      "rocker/r-base:4.1.3"
    )

    container
      .run(script)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(logs => logs.last shouldBe "[1] \"Hello World!\"\n")
  }

  test("Mount a directory with dataset (s)") {
    val container = new SecureContainer[IO](dockerUri, mntFolders)
    val datasetPath = Path.of(getClass.getResource("/").toURI)
    val script = SecureContainer.Command(
      List("cat", "/tmp/data/dataset1.txt"),
      "python:3-alpine"
    )

    container
      .run(script, Data(datasetPath).some)
      .use { case (stateStream, outputStream) => outputStream.compile.toList }
      .asserting(logs => logs.mkString("\n") shouldBe "Hello World! I'm dataset 1!")
  }
