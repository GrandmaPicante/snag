package org.snag.task

import org.snag.service.TheMovieDB
import org.snag.Logging.log
import org.snag.model.Movie

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class MovieMetadataFetcher(theMovieDB: TheMovieDB)(implicit ec: ExecutionContext) {

  def fetchMetadata(movie: Movie): Unit =
    theMovieDB.fetchMovieInfo(movie.id) onComplete {
      case Success(rawInfo) =>
        val cookedInfo = Movie.Metadata(
          title = rawInfo.title,
          alternateTitles = Set(rawInfo.original_title) - rawInfo.title,
          yearReleased = rawInfo.release_date.split('-').head.toInt,
          runtimeMinutes = rawInfo.runtime
        )
        movie.metadata.set(cookedInfo)
        log.warn(s"loaded movie metadata for ${movie}")
      case Failure(ex) =>
        log.warn(s"unable to load movie metadata for ${movie}: $ex")
    }
}
