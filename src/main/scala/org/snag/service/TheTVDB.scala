package org.snag.service

import akka.actor.ActorSystem
import org.snag.{Configuration, HttpClient}
import spray.client.pipelining._
import spray.http.Uri.Query
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.httpx.UnsuccessfulResponseException
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsonFormat}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class AuthenticationInfo(apikey: String, userkey: String)
case class Token(token: String)
case class Links(first: Int = 1, last: Int = 1, next: Option[Int] = None, previous: Option[Int] = None)
case class Page[T](links: Links = Links(), data: List[T] = List.empty)
case class SeriesInfo(seriesId: String, seriesName: String, status: String)
case class EpisodeInfo(airedSeason: Int, airedEpisodeNumber: Int, firstAired: Option[String], episodeName: Option[String])

object TheTVDB {
  implicit val AuthenticationInfoFormat = jsonFormat2(AuthenticationInfo)
  implicit val TokenFormat = jsonFormat1(Token)
  implicit val SeriesInfoFormat = jsonFormat3(SeriesInfo)
  implicit val EpisodeInfoFormat = jsonFormat4(EpisodeInfo)
  implicit val LinksFormat = jsonFormat4(Links)
  implicit def PageFormat[T :JsonFormat] = jsonFormat2(Page[T])
}

import TheTVDB._

class TheTVDB(cfg: Configuration.TheTVDB)(implicit ac: ActorSystem) extends HttpClient {
  override val actorSystem = ac
  private implicit val executionContext = actorSystem.dispatcher

  private val BASEURL = "https://api.thetvdb.com"
  private var tokenOption: Option[String] = None

  private val verbose = false

  private def getToken: Future[String] = tokenOption map { t =>
    Future.successful(t)
  } getOrElse {
    val p = http ~> unmarshal[Token]
    p(Post(s"$BASEURL/login",AuthenticationInfo(cfg.apiKey,cfg.userKey))) map { rsp =>
      tokenOption = Some(rsp.token)
      rsp.token
    }
  }

  def fetchSeriesInfo(seriesId: Int) = getToken flatMap { token =>
    val p = addHeader("Authorization",s"Bearer $token") ~> http ~> unmarshal[JsObject]

    p(Get(s"$BASEURL/series/$seriesId")) map { rsp =>
      rsp.fields("data").convertTo[SeriesInfo]
    }
  }

  def fetchSeriesEpisodes(seriesId: Int) =
    pageEpisodes(s"$BASEURL/series/$seriesId/episodes")

  def fetchSeasonEpisodes(seriesId: Int, airedSeason: Int) =
    pageEpisodes(Uri(s"$BASEURL/series/$seriesId/episodes/query?airedSeason=$airedSeason"))


  private[this] def pageEpisodes(uri:Uri) = {
    val token = Await.result(getToken, Duration.Inf)
    val p = addHeader("Authorization",s"Bearer $token") ~> http ~> unmarshal[Page[EpisodeInfo]]

    // The first page tells us how many more pages there are and gives of the first batch of items.
    // If it's a 404, it either means that the series ID is invalid or that the season doesn't exist.  Either way,
    // return an empty iterator.

    // TODO: There really ought to be a way to get the number of seasons for a series without a 404.

    val firstPage =
      try {
        Await.result(p(Get(uri)), Duration.Inf)
      } catch {
        case ex: UnsuccessfulResponseException if ex.response == StatusCodes.NotFound =>
          println(s"CAUGHT: $ex")
          Page() // Just return an empty page
        case ex: Exception =>
          println(s"CATCHALL: $ex")
          Page()
      }

    // TODO: Make a lazy iterator of iterators for the rest of the pages.

    val subsequentPages = Range.inclusive(2,firstPage.links.last) map { pageNum =>
      Await.result(p(Get(uri.copy(query = Query(uri.query.toMap + ( "page" -> pageNum.toString ))))),Duration.Inf)
    }

    ( Iterator(firstPage) ++ subsequentPages ).map(_.data).flatten
  }
}
