package org.snag.tv

import org.snag.model.Episode
import org.snag.Logging.log
import org.snag.tv.MissingEpisoder.MissingEpisodeDetected
import rx.lang.scala.Observable
import rx.lang.scala.subjects.PublishSubject

object MissingEpisoder {
  case class MissingEpisodeDetected(episode: Episode)
}

class MissingEpisoder(installer: EpisodeInstaller) {

  private[this] val subject = PublishSubject[MissingEpisodeDetected]
  val events: Observable[MissingEpisodeDetected] = subject

  // Check to see if a specified episode already has a media file in the library.  If it does not, emit an event.

  def check(episode: Episode): Unit = {
    log.debug(s"checking for installed media for $episode")

    // TODO: handle old v. new episodes (or think of a better way to model it)
    val episodeWanted = episode.config.get.flatMap(_.wanted)
    val seasonWanted = episode.season.config.get.flatMap(_.wanted)
    val seriesWanted = episode.season.series.config.get.map(_.wantNewEpisodes)

    if ( episodeWanted.getOrElse(seasonWanted.getOrElse(seriesWanted.getOrElse(false))) ) {
      // Episode is wanted.  See if it's already installed.
      installer.getInstalled(episode) match {
        case Some(p) =>
          log.debug(s"episode $episode already installed at $p")
        case None =>
          log.debug(s"missing media file for episode $episode")
          subject.onNext(MissingEpisodeDetected(episode))
      }
    }
  }
}
