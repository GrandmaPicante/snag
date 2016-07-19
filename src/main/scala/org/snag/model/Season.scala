package org.snag.model

import java.io.File
import FileUtils._
import org.snag.model.DirectoryBackedMap.ItemInstantiated
import org.snag.model.FileBackedValue.Update
import rx.lang.scala.Observable
import spray.json.DefaultJsonProtocol._

object Season {
  case class Config(wanted: Option[Boolean] = None)

  object Config {
    implicit val jsonFormat = jsonFormat1(Config.apply)
  }

  case class Metadata(episodeCount: Int)

  object Metadata {
    implicit val jsonFormat = jsonFormat1(Metadata.apply)
  }

  sealed trait Event
  case class ConfigChanged(episode: Season) extends Event
  case class EpisodeInstantiated(search: Episode) extends Event
  case class SearchInstantiated(search: TorrentSearch[Season]) extends Event
  case class DownloadInstantiated(search: TorrentDownload[Season]) extends Event
}

import Season._

class Season private[model] (val series: Series, val id: Int, dir: File) {
  val config = new FileBackedValue(dir / "config.json", Config.jsonFormat)
  val metadata = new FileBackedValue(dir / "metadata.json", Metadata.jsonFormat)

  val episodes = new DirectoryBackedMap[Episode](dir / "episode")(new Episode(this, _, _) )
  val searches = new DirectoryBackedMap(dir / "search")(new TorrentSearch(this, _, _))
  val downloads = new DirectoryBackedMap(dir / "download")(new TorrentDownload(this, _, _))

  val events: Observable[Event] = {
    val configEvents = config.events.map {
      case Update(cfg) => ConfigChanged(this)
    }

    val episodeEvents = episodes.events map {
      case ItemInstantiated(e) => EpisodeInstantiated(e)
    }

    val searchEvents = searches.events map {
      case ItemInstantiated(s) => SearchInstantiated(s)
    }

    val downloadEvents = downloads.events map {
      case ItemInstantiated(dl) => DownloadInstantiated(dl)
    }

    configEvents merge episodeEvents merge searchEvents merge downloadEvents
  }

  /*
  val events: Observable[Event] = {
    val episodeEvents = episodes.events.map {
      case ItemAdded(id, e) => EpisodeAdded(id, e)
      case ItemRemoved(id, e) => EpisodeDeleted(id, e)
    }

    episodeEvents
  }
*/
  override def toString = s"Season#$id"
}
