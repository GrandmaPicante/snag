package org.snag.model

import java.io.File
import org.snag.model.DirectoryBackedMap.ItemInstantiated
import org.snag.model.FileBackedValue.Update
import rx.lang.scala.Observable
import spray.json.DefaultJsonProtocol._
import FileUtils._

object Series {

  case class Config(startSeason: Option[Int] = None,
                    stopSeason: Option[Int] = None,
                    wantNewEpisodes: Boolean = true,
                    wantOldEpisodes: Boolean = false)

  object Config {
    implicit val jsonFormat = jsonFormat4(Config.apply)
  }

  case class Metadata(title: String)

  object Metadata {
    implicit val jsonFormat = jsonFormat1(Metadata.apply)
  }

  sealed trait Event
  case class MetadataChanged(series: Series) extends Event
  case class ConfigChanged(series: Series) extends Event
  case class SeasonInstantiated(season: Season) extends Event
  case class SearchInstantiated(search: TorrentSearch[Series]) extends Event
  case class DownloadInstantiated(search: TorrentDownload[Series]) extends Event
}

import Series._

class Series private[model] (val id: Int, dir: File) {
  val metadata = new FileBackedValue(dir / "metadata.json", Metadata.jsonFormat)
  val config = new FileBackedValue(dir / "config.json", Config.jsonFormat)

  val seasons = new DirectoryBackedMap(dir / "season")(new Season(this,_,_))
  val searches = new DirectoryBackedMap(dir / "search")(new TorrentSearch(this, _, _))
  val downloads = new DirectoryBackedMap(dir / "download")(new TorrentDownload(this, _, _))

  val events: Observable[Event] = {
    val configEvents = config.events.map {
      case Update(cfg) => ConfigChanged(this)
    }

    val metadataEvents = metadata.events.map {
      case Update(md) => MetadataChanged(this)
    }

    val seasonEvents = seasons.events map {
      case ItemInstantiated(s) => SeasonInstantiated(s)
    }

    val searchEvents = searches.events map {
      case ItemInstantiated(s) => SearchInstantiated(s)
    }

    val downloadEvents = downloads.events map {
      case ItemInstantiated(dl) => DownloadInstantiated(dl)
    }

    configEvents merge metadataEvents merge seasonEvents merge searchEvents merge downloadEvents
  }

  override def toString = s"Series#$id"
}
