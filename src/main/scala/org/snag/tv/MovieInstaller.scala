package org.snag.tv

import java.io.File
import org.snag.model.Movie
import org.snag.model.FileUtils._
import org.snag.Logging.log

class MovieInstaller(dir: File) {
  private val videoExtensions = Seq("mkv","mp4","mpg","wmv")

  def targetPath(movie: Movie, extension: String): Option[File] = movie.metadata.get match {
    case Some(md) =>
      val sortTitle =
        if ( md.title.startsWith("The ") )
          md.title.stripPrefix("The ") + ", The"
        else
          md.title

      Some(dir / s"$sortTitle (${md.yearReleased})" / s"${md.title}.${extension}")

    case None =>
      log.debug(s"not enough metadata to build path for $movie, ignoring")
      None
  }

  def getInstalled(movie: Movie) = {
    val existingTargetPaths =
      for {
        ext <- videoExtensions
        path <- targetPath(movie, ext) if ( path.exists )
      } yield {
        path
      }

    existingTargetPaths.headOption
  }
}
