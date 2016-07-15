package org.snag

import java.io.File
import akka.actor.{Props, ActorSystem}
import akka.io.IO
import org.snag.model._
import org.snag.task.{MovieMetadataFetcher, EpisodeSearcher, SeriesMetadataFetcher}
import org.snag.service.{TheMovieDB, TheTVDB, TorrentDay}
import org.snag.tv.{MissingEpisoder, EpisodeInstaller}
import spray.can.Http
import Logging.log
import FileUtils._

object Snag {

  implicit val actorSystem = ActorSystem("default")
  implicit val executionContext = actorSystem.dispatcher

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run() = {
      log.info("System is shutting down...")
      actorSystem.terminate()
    }
  })

  val home = new File("target/home")

  val config = Configuration.load(home / "snag.cfg")

  val torrentDay = new TorrentDay(config.torrentDay)

  val thetvdb = new TheTVDB(config.theTvDb)

  val themoviedb = new TheMovieDB(config.theMovieDb)

  val seriesMetadataFetcher = new SeriesMetadataFetcher(thetvdb)

  val movieMetadataFetcher = new MovieMetadataFetcher(themoviedb)

  val installer = new EpisodeInstaller(new File("target/media_library"))

  val missingEpisoder = new MissingEpisoder(installer)

  val universe = new MediaUniverse(home)

  val dataBucket = new DataBucket(universe, seriesMetadataFetcher, torrentDay, thetvdb)

  def main(args:Array[String]) {

    missingEpisoder.events foreach { med =>
      val es = new EpisodeSearcher(med.episode, torrentDay)
      es.search()
    }

    def onEpisodeInstantiated(episode: Episode) = {
      episode.events foreach {
        case Episode.MetadataChanged(meta) =>
          missingEpisoder.check(episode)
        case _ => // NOOP
      }
    }

    def onSeasonInstantiated(season: Season) =
      season.events foreach {
        case Season.EpisodeInstantiated(episode) =>
          log.debug(s"episode instantiated: $episode")
          onEpisodeInstantiated(episode)
        case Season.ConfigChanged(cfg) =>
          log.debug(s"season configuration updated: $cfg")
//          metadataFetcher.fetchEpisodes(season.series) // TODO: target this to the specific season?
        case _ => // NOOP
      }

    def onSeriesInstantiated(series: Series) =
      series.events foreach {
        case Series.SeasonInstantiated(season) =>
          log.debug(s"season instantiated: $season")
          onSeasonInstantiated(season)
        case Series.ConfigChanged(cfg) =>
          log.debug(s"series configuration updated: $cfg")
          seriesMetadataFetcher.fetchMetadata(series)
          seriesMetadataFetcher.fetchEpisodes(series)
        case _ => // NOOP
      }

    def onMovieInstantiated(movie: Movie) = {
      movieMetadataFetcher.fetchMetadata(movie)

      movie.events foreach {
        case Movie.ConfigChanged(movie) =>
          // trigger a new search here (maybe, if quality changed, etc.)
        case Movie.MetadataChanged(movie) =>
          // trigger a new search here (maybe, if name/year changed, etc.)
        case _ => // NOOP
      }
    }

    universe.events foreach {
      case MediaUniverse.SeriesInstantiated(series) =>
        log.debug(s"series instantiated: $series")
        onSeriesInstantiated(series)
      case MediaUniverse.MovieInstantiated(movie) =>
        log.debug(s"movie instantiated: $movie")
        onMovieInstantiated(movie)
    }


    val routes = actorSystem.actorOf(Props(classOf[Routes],dataBucket))

    IO(Http) ! Http.Bind(routes,interface = "localhost",port = 8888)
  }
}
