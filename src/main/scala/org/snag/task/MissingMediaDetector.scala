package org.snag.task

import org.snag.Logging._
import org.snag.model.Movie
import org.snag.tv.MovieInstaller
import rx.lang.scala.Observable
import rx.lang.scala.subjects.PublishSubject

object MissingMediaDetector {
  case class MissingMovieDetected(movie: Movie)
}

import org.snag.task.MissingMediaDetector._

class MissingMediaDetector(installer: MovieInstaller) {

  private[this] val subject = PublishSubject[MissingMovieDetected]
  val events: Observable[MissingMovieDetected] = subject

  // Check to see if a specified episode already has a media file in the library.  If it does not, emit an event.

  def check(movie: Movie): Unit = {
    log.debug(s"checking for installed media for $movie")

    if ( movie.config.get.map(_.wanted) == Some(true) ) {
      // Movie is wanted.  See if it's already installed.
      installer.getInstalled(movie) match {
        case Some(p) =>
          log.debug(s"movie $movie already installed at $p")
        case None =>
          log.debug(s"missing media file for movie $movie")
          subject.onNext(MissingMovieDetected(movie))
      }
    }
  }
}
