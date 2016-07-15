package org.snag.service

import akka.actor.ActorSystem
import org.snag.{Configuration, HttpClient}
import spray.client.pipelining._
import spray.http.Uri
import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

case class MovieInfo(id: Int,
                     title: String,
                     original_title: String,
                     release_date: String,
                     runtime: Int)

object TheMovieDB {
  implicit val MovieInfoFormat = jsonFormat5(MovieInfo)
}

import TheMovieDB._

class TheMovieDB(cfg: Configuration.TheMovieDB)(implicit ac: ActorSystem) extends HttpClient {
  override val actorSystem = ac
  private implicit val executionContext = actorSystem.dispatcher

  private val BASEURL = "http://api.themoviedb.org/3"

  def fetchMovieInfo(movieId: Int) = {
    val p = http ~> unmarshal[MovieInfo]

    val uri = Uri(s"$BASEURL/movie/$movieId")

    // Dropping support for alternate titles for now.  It seems like the original title and the english title are enough.
    p(Get(uri.copy(query = Uri.Query("api_key" -> cfg.apiKey))))//,"append_to_response" -> "alternative_titles"))))
  }
}
