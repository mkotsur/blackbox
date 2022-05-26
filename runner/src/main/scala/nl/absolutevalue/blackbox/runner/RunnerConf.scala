package nl.absolutevalue.blackbox.runner

import cats.effect.Sync
import nl.absolutevalue.blackbox.runner.RunnerConf.RemoteFolders
import pureconfig.ConfigSource

import java.nio.file.Path
import pureconfig.generic.derivation.default.*
import pureconfig.*

import java.net.URI

object RunnerConf {
  case class RemoteFolders(code: Path, data: Path, output: Path) derives ConfigReader

  def configSrc = ConfigSource.default.at("blackbox.runner")

  def loadF[F[_]: Sync]: F[RunnerConf] = {
    val res = configSrc.load[RunnerConf].left.map(ff => new RuntimeException(ff.prettyPrint()))
    Sync[F].fromEither[RunnerConf](res)
  }

}

case class RunnerConf(
    outputsPath: Path,
    dataSamplesPath: Path,
    dockerUri: URI,
    mountFolders: RemoteFolders,
    dockerImages: Map[String, String]
) derives ConfigReader
