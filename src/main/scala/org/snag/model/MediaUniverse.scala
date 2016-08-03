package org.snag.model

import java.io.File
import org.snag.model.DirectoryBackedMap.ItemInstantiated
import org.snag.model.FileUtils._
import rx.lang.scala.Observable

object MediaUniverse {
  sealed trait Event
  case class SeriesInstantiated(series: Series) extends Event
  case class MovieInstantiated(movie: Movie) extends Event
}

import MediaUniverse._

class MediaUniverse(dir: File) {
  val movies = new DirectoryBackedMap[Int, Movie](dir / "movies")(new Movie(_, _))
  val tv = new DirectoryBackedMap[Int, Series](dir / "tv")(new Series(_, _))

  val events: Observable[Event] = {
    val seriesEvents = tv.events map {
      case ItemInstantiated(s) => SeriesInstantiated(s)
    }

    val movieEvents = movies.events map {
      case ItemInstantiated(dl) => MovieInstantiated(dl)
    }

    seriesEvents merge movieEvents
  }
}
