package nl.absolutevalue.blackbox

import cats.effect.kernel.Resource.Pure
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, SyncIO}
import cats.effect.MonadCancel
import cats.implicits.*
import cats.syntax.*
import nl.absolutevalue.blackbox.SecureContainer
import org.scalatest.funsuite.{AnyFunSuite, AsyncFunSuite}
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import docker.DockerContainer.State

class SecureContainerTest extends AsyncFunSuite with AsyncIOSpec with Matchers:

  implicit val logger: SelfAwareStructuredLogger[SyncIO] = Slf4jLogger.getLogger[SyncIO]

  private val commandHelloWorld = List("python", "-c", "\"print('Hello World!')\"")

  test("Should create container") {
    val container = new SecureContainer[SyncIO]
    val script = SecureContainer.Script(List("test"), "python:3.11.0a5-alpine3.15")

    MonadCancel[SyncIO]
      .bracket(container.create(script))(_.state.pure[SyncIO])(container.destroy)
      .asserting(_ shouldBe State.Created)
  }

  test("Should run script in a container") {
    val container = new SecureContainer[SyncIO]
    val script = SecureContainer.Script(commandHelloWorld, "python:3-alpine")

    container
      .run(script)
      .compile
      .last
      .asserting(_.get shouldBe a[State.ExitSuccess])

    //TODO: the container keeps hanging

  }
