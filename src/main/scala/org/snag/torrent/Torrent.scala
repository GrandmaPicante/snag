package org.snag.torrent

import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import com.turn.ttorrent.client.Client
import org.snag.model.FileBackedValue
import org.snag.model.FileUtils._
import org.snag.service.TorrentDay.TorrentInfo
import spray.json.DefaultJsonProtocol._
import spray.json._

object Torrent {
  sealed trait TorrentStatus
  case object Active extends TorrentStatus
  case object Successful extends TorrentStatus
  case object Failed extends TorrentStatus

  object Metadata {
    implicit val jsonFormat = jsonFormat2(Metadata.apply)
  }
  case class Metadata(torrentInfo: TorrentInfo, expectedContent:Seq[String])

  implicit object TorrentJsonFormat extends RootJsonWriter[Torrent] {
    def write(torrent: Torrent) =
      JsObject(List(
        Some("id" -> JsString(torrent.id)),
        Some("status" -> JsString(torrent.status.toString)),
        torrent.metadata.get map { metadata => "metadata" -> metadata.toJson }
      ).flatten: _*)
  }
}

class Torrent private[torrent] (val id: String, dir: File, getClient:(File, File) => Client) {
  import Torrent._

  val metadata = new FileBackedValue(dir / "metadata.json", Metadata.jsonFormat)
  private val torrentFile = dir / s"${id}.torrent"
  lazy val client = getClient(torrentFile, mkdir(dir / "data"))

  def this(id: String, dir: File, info: TorrentInfo, expectedContent:Seq[String], sourceTorrentFile: File, getClient:(File, File) => Client) = {
    this(id, dir, getClient)
    metadata.set(Metadata(info, expectedContent))
    Files.copy(sourceTorrentFile.toPath, torrentFile.toPath, StandardCopyOption.REPLACE_EXISTING)
  }

  def status: TorrentStatus = {
    client.getState match {
      case Client.ClientState.DONE => Successful
      case Client.ClientState.SEEDING => Successful
      case Client.ClientState.WAITING => Active
      case Client.ClientState.VALIDATING => Active
      case Client.ClientState.SHARING => Active
      case Client.ClientState.ERROR => Failed
    }
  }

  def start: Unit = {
    client.download()
  }
}

