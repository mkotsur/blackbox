package nl.absolutevalue.blackbox.datasets

import cats.{Monad, MonadThrow}
import cats.effect.Sync

import java.io.File
import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import cats.nio.file.Files
import cats.implicits.*
import nl.absolutevalue.blackbox.util.TempFiles

import java.util.stream.Collectors
import scala.Tuple.FlatMap
import scala.jdk.CollectionConverters._
import cats.implicits._
import java.util.stream.{Stream => JStream}

class Datasets[F[_]: MonadThrow: Sync: Files](dataFolder: Path) {

  def list: F[Seq[(Path, Option[Long])]] = {
    for {
      pathsS: JStream[Path] <- Files[F].list(dataFolder)
      paths = pathsS
        .iterator()
        .asScala
        .toList
      sizeData: List[(Long, Boolean)] <- paths.traverse(p =>
        (Files[F].size(p), Files[F].isRegularFile(p)).tupled
      )

    } yield paths
      .map(p => dataFolder.relativize(p))
      .zip(sizeData.map {
        case (s, true) => s.some
        case _         => None
      })

  }

}
