package nl.absolutevalue.blackbox.util

import cats.effect.{MonadCancel, Resource, Sync}
import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.file.{Files, Path}

object TempFiles {

  private val tmpDirPrefix = "blackbox"

  def tempDir[F[_]: Sync]: Resource[F, Path] =
    Resource.make(Sync[F].delay(Files.createTempDirectory(tmpDirPrefix)))(path =>
      Sync[F].delay(FileUtils.deleteQuietly(path.toFile))
    )

}
