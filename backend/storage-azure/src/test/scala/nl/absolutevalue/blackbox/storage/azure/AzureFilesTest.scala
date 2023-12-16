package nl.absolutevalue.blackbox.storage.azure

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.IORuntime
import com.github.sardine.impl.SardineImpl
import com.github.sardine.{DavResource, Sardine}
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.URI
import java.util
import scala.jdk.CollectionConverters.*
class AzureFilesTest extends AsyncFunSuite with AsyncIOSpec with Matchers with MockitoSugar:

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override implicit lazy val ioRuntime: IORuntime = cats.effect.unsafe.implicits.global

  test("Should return listing") {
    ???
  }
