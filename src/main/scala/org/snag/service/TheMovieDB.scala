package org.snag.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import org.snag.{Configuration, HttpClient}
import spray.client.pipelining._
import spray.http.Uri
import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

object TheMovieDB {
  case class MovieInfo(id: Int,
                       title: String,
                       original_title: String,
                       release_date: LocalDate,
                       runtime: Int)

  case class SeriesInfo(id: Int,
                        name: String,
                        original_name: String,
                        first_air_date: LocalDate,
                        last_air_date: LocalDate,
                        status: String,
                        number_of_seasons: Int,
                        seasons: List[SeasonSummary])

  case class SeasonSummary(season_number: Int,
                           episode_count: Int)

  case class SeasonInfo(season_number: Int,
                        episodes: List[EpisodeInfo])

  case class EpisodeInfo(season_number: Int,
                         episode_number: Int,
                         air_date: LocalDate,
                         name: String)

  implicit object LocalDateFormat extends RootJsonFormat[LocalDate] {
    private[this] val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    override def write(t: LocalDate): JsValue = JsString(t.format(formatter))
    override def read(json: JsValue): LocalDate = json match {
      case JsString(s) => LocalDate.parse(s,formatter)
      case x => throw new IllegalArgumentException(s"invalid LocalDate: $x")
    }
  }

  implicit val MovieInfoFormat = jsonFormat5(MovieInfo)
  implicit val SeasonSummaryFormat = jsonFormat2(SeasonSummary)
  implicit val SeriesInfoFormat = jsonFormat8(SeriesInfo)
  implicit val EpisodeInfoFormat = jsonFormat4(EpisodeInfo)
  implicit val SeasonInfoFormat = jsonFormat2(SeasonInfo)
}

import TheMovieDB._

class TheMovieDB(cfg: Configuration.TheMovieDB)(implicit ac: ActorSystem) extends HttpClient {
  override val actorSystem = ac
  private implicit val executionContext = actorSystem.dispatcher

  private val BASEURL = "http://api.themoviedb.org/3"

  def fetchMovieInfo(movieId: Int) = {
    val p = http ~> unmarshal[MovieInfo]

    val uri = Uri(s"$BASEURL/movie/$movieId")

    // Dropping support for alternative titles for now.  It seems like the original title and the english title are enough.
    p(Get(uri.copy(query = Uri.Query("api_key" -> cfg.apiKey))))//,"append_to_response" -> "alternative_titles"))))
  }

  def fetchSeriesInfo(seriesId: Int) = {
    val p = http ~> unmarshal[TheMovieDB.SeriesInfo]

    val uri = Uri(s"$BASEURL/tv/$seriesId")

    // Dropping support for alternative titles for now.  It seems like the original title and the english title are enough.
    p(Get(uri.copy(query = Uri.Query("api_key" -> cfg.apiKey))))//,"append_to_response" -> "alternative_titles"))))
  }

  def fetchSeasonInfo(seriesId: Int, seasonNumber: Int) = {
    val p = http ~> unmarshal[SeasonInfo]

    val uri = Uri(s"$BASEURL/tv/$seriesId/season/$seasonNumber")

    p(Get(uri.copy(query = Uri.Query("api_key" -> cfg.apiKey))))
  }
}
