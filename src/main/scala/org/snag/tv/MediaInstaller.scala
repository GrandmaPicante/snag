package org.snag.tv

import java.io.File
import org.snag.Logging._
import org.snag.model.FileUtils._
import org.snag.model.{Movie, Episode}

abstract class MediaInstaller[T](dir: File) {
  protected val videoExtensions = Seq("mkv","mp4","mpg","wmv")

  protected def targetPath(t: T, extension: String): Option[File]

  def getInstalled(t: T): Option[File] = {
    val existingTargetPaths =
      for {
        ext <- videoExtensions
        path <- targetPath(t, ext) if ( path.exists )
      } yield {
        path
      }

    existingTargetPaths.headOption
  }
}

class EpisodeInstaller(dir: File) extends MediaInstaller[Episode](dir) {
  override def targetPath(episode: Episode, extension: String): Option[File] =
    for {
      seriesTitle <- episode.season.series.metadata.get.map(_.title)
      episodeTitle <- episode.metadata.get.map(_.title)
    } yield {
      dir / seriesTitle / "%s - %dx%02d - %s.%s".format(seriesTitle,episode.season.id,episode.id,episodeTitle,extension)
    }
}

class MovieInstaller(dir: File) extends MediaInstaller[Movie](dir) {
  override def targetPath(movie: Movie, extension: String): Option[File] =
    movie.metadata.get match {
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
}
