package nl.absolutevalue.blackbox.datasets

import cats.{Monad, MonadThrow}
import cats.effect.{Concurrent, Resource, Sync}

import java.io.File
import java.nio.file.Path as JPath
import scala.jdk.CollectionConverters.*
import fs2.io.file.{Files, Path}
import cats.implicits.*
import nl.absolutevalue.blackbox.util.TempFiles
import scala.collection.immutable.Seq

import java.util.stream.Collectors
import scala.Tuple.FlatMap
import scala.jdk.CollectionConverters.*
import cats.implicits.*
import scala.collection.immutable.Seq

import java.util.stream.Stream as JStream

class Datasets[F[_]: MonadThrow: Sync: Files](dataFolder: JPath) {

  def list: Resource[F, Seq[(JPath, Option[Long])]] =
    Files[F]
      .list(Path.fromNioPath(dataFolder))
      .evalMap(p =>
        (
          dataFolder.relativize(p.toNioPath).pure[F],
          Files[F].isRegularFile(p).flatMap {
            case true  => Files[F].size(p).map(_.some)
            case false => None.pure[F]
          }
        ).tupled
      )
      .compile
      .resource
      .toList

}
