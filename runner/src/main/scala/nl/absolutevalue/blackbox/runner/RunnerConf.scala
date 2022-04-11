package nl.absolutevalue.blackbox.runner

import cats.effect.Sync
import nl.absolutevalue.blackbox.runner.RunnerConf.MountFolders
import pureconfig.ConfigSource

import java.nio.file.Path
import pureconfig.generic.derivation.default.*
import pureconfig.*

import java.net.URI

object RunnerConf {
  case class MountFolders(code: Path, data: Path) derives ConfigReader

  def configSrc = ConfigSource.default.at("blackbox.runner")

  def loadF[F[_]: Sync]: F[RunnerConf] = {
    val res = configSrc.load[RunnerConf].left.map(ff => new RuntimeException(ff.prettyPrint()))
    Sync[F].fromEither[RunnerConf](res)
  }

}

case class RunnerConf(dataSamplesPath: Path, dockerUri: URI, mountFolders: MountFolders)
    derives ConfigReader
