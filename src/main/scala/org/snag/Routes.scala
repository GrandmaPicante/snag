package org.snag

import org.snag.Logging.log
import org.snag.model.FileUtils._
import org.snag.model.{Episode, Movie, Series}
import org.snag.service.TorrentDay.TorrentInfo
import org.snag.task.Wanter
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.routing.{Directive1, HttpServiceActor}

import scala.util.{Failure, Success}

class Routes(dataBucket: DataBucket) extends HttpServiceActor {
  implicit val ec = actorRefFactory.dispatcher

  private[this] val wanter = new Wanter(dataBucket.theMovieDB)

  override def receive = runRoute {
    pathPrefix("api" / "v1" ) {
      path("update") {
        get {
          complete {
            dataBucket.universe.tv.items.values.foreach(wanter.snag)
            StatusCodes.NoContent
          }
        }
      } ~
      pathPrefix("series") {
        pathPrefix(IntNumber) { seriesId =>
          pathEnd {
            put {
              entity(as[Series.Config]) { sc =>
                provide(dataBucket.universe.tv.getOrCreate(seriesId)) { series =>
                  complete {
                    series.config.set(sc)
                    StatusCodes.NoContent
                  }
                }
              }
            }
          } ~
          ifDefined( dataBucket.universe.tv.get(seriesId) ) { series =>
            pathEnd {
              get {
                complete {
                  series.config.get
                }
              } ~
              delete {
                complete {
                  dataBucket.universe.tv.delete(seriesId)
                  StatusCodes.NoContent
                }
              }
            } ~
            path("season" / IntNumber / "episode" / IntNumber ) { (seasonNum, episodeNum) =>
              ifDefined( series.seasons.get(seasonNum) ) { season =>
                ifDefined( season.episodes.get(episodeNum) ) { episode =>
                  import Episode.Config.jsonFormat
                  get {
                    complete(episode.config.get)
                  } ~
                  put {
                    entity(as[Episode.Config]) { ec =>
                      complete {
                        episode.config.set(ec)
                        StatusCodes.NoContent
                      }
                    }
                  }
                }
              }
            }
          }
        }
      } ~
      pathPrefix("movie") {
        pathPrefix(IntNumber) { movieId =>
          pathEnd {
            put {
              entity(as[Movie.Config]) { mc =>
                provide(dataBucket.universe.movies.getOrCreate(movieId)) { movie =>
                  complete {
                    movie.config.set(mc)
                    StatusCodes.NoContent
                  }
                }
              }
            }
          } ~
          ifDefined( dataBucket.universe.movies.get(movieId) ) { movie =>
            pathEnd {
              get {
                complete {
                  movie.config.get
                }
              } ~
              delete {
                complete {
                  dataBucket.universe.movies.delete(movieId)
                  StatusCodes.NoContent
                }
              }
            }
          }
        }
      } ~
      pathPrefix("torrent") {
        path("search") {
          get {
            parameter('query) { query =>
              complete(dataBucket.torrenter.searchByExpectedContent(query).map(_.toJson))
            }
          }
        } ~
        path("(?i)^[0-9a-f]+$".r) { id =>
          ifDefined(dataBucket.torrenter.get(id)) { torrent =>
            complete(torrent)
          }
        } ~
        pathEnd {
          get {
            complete(dataBucket.torrenter.getAll.map(_.toJson))
          } ~
          post {
            // TODO This is a temporary endpoint to directly drive the Torrenter
            entity(as[TorrentInfo]) { info =>
              complete {
                val tempFile = dataBucket.homeDir / "temp" / info.id.toString
                mkdir(tempFile.getParentFile)
                tempFile.createNewFile
                dataBucket.torrentDay.fetch(info, tempFile) onComplete {
                  case Success(_) => {
                    val torrent = dataBucket.torrenter.download(info, Seq(info.title), tempFile)
                    log.info(s"Download of torrent (${info.url}) resulted in id (${torrent.id})")
                  }
                  case Failure(ex) => log.error(s"Torrent download failed (${info.url}): $ex")
                }
                StatusCodes.NoContent
              }
            }
          }
        }
      }
    }
  }

  def ifDefined[A](fn: => Option[A]): Directive1[A] =
    fn match {
      case Some(x) => extract[A](_ => x)
      case None => complete(StatusCodes.NotFound)
    }
}
