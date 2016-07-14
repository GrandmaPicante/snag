package org.snag.tv

import java.io.File
import org.snag.model.FileUtils._
import org.snag.model.Episode

class EpisodeInstaller(dir: File) {
  private val videoExtensions = Seq("mkv","mp4","mpg","wmv")

  def targetPath(episode: Episode, extension: String) =
    for {
      seriesTitle <- episode.season.series.metadata.get.map(_.title)
      episodeTitle <- episode.metadata.get.map(_.title)
    } yield {
      dir / seriesTitle / "%s - %dx%02d - %s.%s".format(seriesTitle,episode.season.id,episode.id,episodeTitle,extension)
    }

  def getInstalled(episode: Episode) = {
    val existingTargetPaths =
      for {
        ext <- videoExtensions
        path <- targetPath(episode, ext) if ( path.exists )
      } yield {
        path
      }

    existingTargetPaths.headOption
  }
}
