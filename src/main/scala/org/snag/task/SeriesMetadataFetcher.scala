package org.snag.task

import org.snag.model.{Episode, Series}
import org.snag.service.TheTVDB
import scala.concurrent.{Promise, Future, ExecutionContext}
import scala.util.{Failure, Success}
import org.snag.Logging.log

class SeriesMetadataFetcher(thetvdb: TheTVDB)(implicit ec: ExecutionContext) {
  // TODO: Don't sync things that don't need to be synced (eventually)

  def fetchMetadata(series: Series): Unit =
    thetvdb.fetchSeriesInfo(series.id) onComplete {
      case Success(rawInfo) =>
        val cookedInfo = Series.Metadata(rawInfo.seriesName)
        series.metadata.set(cookedInfo)
        log.warn(s"loaded series metadata for ${series}")
      case Failure(ex) =>
        log.warn(s"unable to load series metadata for ${series}: $ex")
    }

  def fetchEpisodes(series: Series): Unit =
    series.config.get map { cfg =>
      val start = cfg.startSeason.getOrElse(1)
      val stop = cfg.stopSeason

      val seasonRange = stop match {
        case Some(n) => Iterator.range(start,stop.get)
        // Iterate until we hit an empty season if there's no end
        case None => Iterator.from(start)
      }

      seasonRange map { seasonNum =>
        val rawEpisodes = thetvdb.fetchSeasonEpisodes(series.id, seasonNum)
        (seasonNum, rawEpisodes)
      } takeWhile { case (_,eps) =>
        ! eps.isEmpty
      } foreach { case (seasonNum, rawEpisodes) =>
        val season = series.seasons.getOrCreate(seasonNum)

        rawEpisodes foreach { raw =>
          val cooked = Episode.Metadata(raw.firstAired,raw.episodeName)
          val ep = season.episodes.getOrCreate(raw.airedEpisodeNumber)
          ep.metadata.set(cooked)
        }
      }
    }
}
