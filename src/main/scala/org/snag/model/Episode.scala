package org.snag.model

import java.io.File
import java.time.LocalDate

import org.snag.model.DirectoryBackedMap.ItemInstantiated
import org.snag.model.FileBackedValue.Update
import rx.lang.scala.Observable
import spray.json.DefaultJsonProtocol._
import FileUtils._

object Episode {

  case class Config(wanted: Option[Boolean] = None)

  object Config {
    implicit val jsonFormat = jsonFormat1(Config.apply)
  }

  case class Metadata(firstAired: LocalDate,
                      title: String)

  object Metadata {
    import org.snag.service.TheMovieDB.LocalDateFormat
    implicit val jsonFormat = jsonFormat2(Metadata.apply)
  }

  sealed trait Event
  case class MetadataChanged(episode: Episode) extends Event
  case class ConfigChanged(episode: Episode) extends Event
  case class SearchInstantiated(search: TorrentSearch[Episode]) extends Event
  case class DownloadInstantiated(search: TorrentDownload[Episode]) extends Event
}

import Episode._

class Episode private[model] (val season: Season, val id: Int, dir: File) {
  val metadata = new FileBackedValue(dir / "metadata.json", Metadata.jsonFormat)
  val config = new FileBackedValue(dir / "config.json", Config.jsonFormat)

  val searches = new SequentialDirectoryBackedMap[Int, TorrentSearch[Episode]](dir / "search")(new TorrentSearch[Episode](this, _, _))
  val downloads = new DirectoryBackedMap[Int,TorrentDownload[Episode]](dir / "download")(new TorrentDownload(this, _, _))

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

  override def toString = s"Episode#$id"
}
