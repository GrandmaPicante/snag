package org.snag.task

import org.snag.Logging._
import org.snag.model.{Episode, Season, Movie, Series}
import org.snag.service.TheMovieDB
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._

class Wanter(theMovieDB: TheMovieDB)(implicit ec: ExecutionContext) {
  def snag(movie: Movie): Unit = {

  }

  def snag(series: Series): Unit = {

    // Refresh the series metadata (if necessary)
    updateSeriesMetadata(series) map { _ =>
      // Refresh each season's metadata (if necessary)

      val wantedRange = {
        val cfg = series.config.get.get
        val start = cfg.startSeason.getOrElse(1)
        val stop = cfg.stopSeason.getOrElse(series.metadata.get.get.seasonCount)
        Range.inclusive(start, stop)
      }

      val seasonFutures =
        wantedRange foreach { seasonNumber =>
          val season = series.seasons.getOrCreate(seasonNumber)
          log.debug(s"updating metadata for $season")
          updateSeasonMetadata(season)
        }

      // TODO: delete unwanted season directories that are there (because we used to want them)?
    }

    // Determine which of the episodes are needed (and are not already in progress)

    // Decide whether to search for individual episodes or season/series packs.

    // Do a search to find the episodes that we're looking for.
  }

  private[this] val minimumRefreshInterval = 1 day
  private[this] val maximumRefreshInterval = 7 days

  def updateSeriesMetadata(series: Series): Future[Unit] = {
    // Decide if we need to refresh the metadata and make a request out.  This will depend on what the timestamp on
    // the metadata is and the configured refresh intervals.  If the minimum interval has passed, go ahead an refresh
    // when this call is made.

    val now = System.currentTimeMillis
    val age = now - series.metadata.lastUpdatedAt
    if ( age > minimumRefreshInterval.toMillis ) {
      fetchSeriesMetadata(series)
    } else {
      log.debug(s"not updating metadata for $series because it's recent enough")
      Future.successful(())
    }
  }

  private[this] def fetchSeriesMetadata(series: Series): Future[Unit] =
    theMovieDB.fetchSeriesInfo(series.id) map { rawInfo =>

      // Save the series metadata that we fetched.

      val cookedInfo =
        Series.Metadata(
          title = rawInfo.name,
          originalTitle = rawInfo.original_name,
          seasonCount = rawInfo.number_of_seasons,
          firstAired = rawInfo.first_air_date,
          lastAired = rawInfo.last_air_date,
          ended = rawInfo.status == "Ended"
        )
      series.metadata.set(cookedInfo)
      log.warn(s"loaded series metadata for ${series}")

    } recoverWith {
      case ex =>
        log.warn(s"unable to load series metadata for ${series}: $ex")
        throw ex
    }

  def updateSeasonMetadata(season: Season): Future[Unit] = {
    // Decide if we need to refresh the metadata and make a request out.  This will depend on what the timestamp on
    // the metadata is and the configured refresh intervals.  If the minimum interval has passed, go ahead an refresh
    // when this call is made.

    val now = System.currentTimeMillis
    val age = now - season.metadata.lastUpdatedAt
    if ( age > minimumRefreshInterval.toMillis ) {
      fetchSeasonMetadata(season)
    } else {
      log.debug(s"not updating metadata for $season because it's recent enough")
      Future.successful()
    }
  }

  def fetchSeasonMetadata(season: Season): Future[Unit] =
    theMovieDB.fetchSeasonInfo(season.series.id, season.id) map { raw =>

      // Store the season metadata (mostly for the timestamp it give us)

      season.metadata.set(Season.Metadata(raw.episodes.length))

      // Store the metadata for each episode

      raw.episodes foreach { re =>
        val cooked = Episode.Metadata(re.air_date,re.name)
        val ep = season.episodes.getOrCreate(re.episode_number)
        ep.metadata.set(cooked)
      }

    } recoverWith {
      case ex =>
        log.warn(s"unable to load season metadata for ${season}: $ex")
        throw ex
    }

}
