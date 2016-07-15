package org.snag

import org.snag.model.{Movie, Episode, Series}
import spray.http.StatusCodes
import spray.routing.directives.OnSuccessFutureMagnet
import spray.routing.{Directive1, Route, HttpServiceActor}
import spray.httpx.SprayJsonSupport._
import org.snag.Logging.log

import scala.concurrent.Future

class Routes(dataBucket: DataBucket) extends HttpServiceActor {
  implicit val ec = actorRefFactory.dispatcher

  override def receive = runRoute {
    pathPrefix("api" / "v1" ) {
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
