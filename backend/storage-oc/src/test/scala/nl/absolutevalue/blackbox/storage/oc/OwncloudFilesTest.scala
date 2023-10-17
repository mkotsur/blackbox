package nl.absolutevalue.blackbox.storage.oc

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.IORuntime
import com.github.sardine.impl.SardineImpl
import com.github.sardine.{DavResource, Sardine}
import nl.absolutevalue.blackbox.storage.oc.conf.OwncloudConf.WebdavBase
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.URI
import java.util
import scala.jdk.CollectionConverters.*
class OwncloudFilesTest extends AsyncFunSuite with AsyncIOSpec with Matchers with MockitoSugar:

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override implicit lazy val ioRuntime: IORuntime = cats.effect.unsafe.implicits.global

  test("Should return listing") {

    val sardineMock: Sardine = new SardineImpl() {

      override def list(url: String, depth: Int): util.List[DavResource] = {
        val res = mock[DavResource]
        List(res).asJava
      }
    }

    val wdBase = WebdavBase(URI.create("http://example.com"), "/123")
    val res = OwncloudFiles.listTopLevel[IO]("/").run(sardineMock, wdBase)
    res.asserting(_ shouldBe empty)
  }
