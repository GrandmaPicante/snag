package org.snag

import org.snag.model.{Episode, Movie, Series}
import org.snag.task.Wanter
import spray.http.StatusCodes
import spray.routing.directives.OnSuccessFutureMagnet
import spray.routing.{Directive1, HttpServiceActor, Route}
import spray.httpx.SprayJsonSupport._
import org.snag.Logging.log
import org.snag.service.TorrentDay.TorrentInfo
import org.snag.model.FileUtils._

import scala.concurrent.Future
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
      path("download") {
        // TODO This is a temporary endpoint to directly drive the Torrenter
        post {
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
      }
    }
  }

  def ifDefined[A](fn: => Option[A]): Directive1[A] =
    fn match {
      case Some(x) => extract[A](_ => x)
      case None => complete(StatusCodes.NotFound)
    }
}
