package org.snag.model

import java.io.File
import java.time.LocalDate
import org.snag.model.DirectoryBackedMap.ItemInstantiated
import org.snag.model.FileBackedValue.Update
import rx.lang.scala.Observable
import spray.json.DefaultJsonProtocol._
import FileUtils._

object Movie {

  // TODO: should have quality, etc. here
  case class Config(wanted: Boolean = true)

  object Config {
    implicit val jsonFormat = jsonFormat1(Config.apply)
  }

  case class Metadata(title: String,
                      alternateTitles: Set[String],
                      yearReleased: Int,
                      runtimeMinutes: Int)

  object Metadata {
    implicit val jsonFormat = jsonFormat4(Metadata.apply)
  }

  sealed trait Event
  case class MetadataChanged(series: Movie) extends Event
  case class ConfigChanged(series: Movie) extends Event
  case class SearchInstantiated(search: TorrentSearch[Movie]) extends Event
  case class DownloadInstantiated(search: TorrentDownload[Movie]) extends Event
}

import Movie._

class Movie private[model] (val id: Int, dir: File) {
  val metadata = new FileBackedValue(dir / "metadata.json", Metadata.jsonFormat)
  val config = new FileBackedValue(dir / "config.json", Config.jsonFormat)

  val searches = new DirectoryBackedMap(dir / "search")(new TorrentSearch(this, _, _))
  val downloads = new DirectoryBackedMap(dir / "download")(new TorrentDownload(this, _, _))

  val events: Observable[Event] = {
    val configEvents = config.events.map {
      case Update(cfg) => ConfigChanged(this)
    }

    val metadataEvents = metadata.events.map {
      case Update(md) => MetadataChanged(this)
    }

    val searchEvents = searches.events map {
      case ItemInstantiated(s) => SearchInstantiated(s)
    }

    val downloadEvents = downloads.events map {
      case ItemInstantiated(dl) => DownloadInstantiated(dl)
    }

    configEvents merge metadataEvents merge searchEvents merge downloadEvents
  }

  override def toString = s"Movie#$id"
}
