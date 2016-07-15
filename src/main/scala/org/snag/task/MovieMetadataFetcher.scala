package org.snag.task

import org.snag.service.TheMovieDB
import org.snag.Logging.log
import org.snag.model.Movie
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.concurrent.duration._

class MovieMetadataFetcher(theMovieDB: TheMovieDB)(implicit ec: ExecutionContext) {
  private[this] val minimumRefreshInterval = 1 day
  private[this] val maximumRefreshInterval = 7 days

  def updateMetadata(movie: Movie): Unit = {
    // Decide if we need to refresh the metadata by making a request out.  This will depend on what the timestamp on
    // the metadata is and the configured refresh intervals.  If the minimum interval has passed, go ahead an refresh
    // when this call is made.

    val now = System.currentTimeMillis
    val age = now - movie.metadata.lastUpdatedAt
    if ( age > minimumRefreshInterval.toMillis )
      fetchMetadata(movie)
    else
      log.debug(s"not updating metadata for $movie because it's recent enough")
  }

  def fetchMetadata(movie: Movie): Unit = {
    theMovieDB.fetchMovieInfo(movie.id) onComplete {
      case Success(rawInfo) =>
        val cookedInfo = Movie.Metadata(
          title = rawInfo.title,
          alternateTitles = Set(rawInfo.original_title) - rawInfo.title,
          yearReleased = rawInfo.release_date.split('-').head.toInt,
          runtimeMinutes = rawInfo.runtime
        )
        movie.metadata.set(cookedInfo)
        log.warn(s"loaded movie metadata for $movie")
      case Failure(ex) =>
        log.warn(s"unable to load movie metadata for $movie: $ex")
    }
  }
}
