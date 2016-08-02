package org.snag.torrent

import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import com.turn.ttorrent.client.Client
import org.snag.model.FileBackedValue
import org.snag.model.FileUtils._
import org.snag.service.TorrentDay.TorrentInfo
import spray.json.DefaultJsonProtocol._

object Torrent {
  sealed trait TorrentStatus
  case object Active extends TorrentStatus
  case object Successful extends TorrentStatus
  case object Failed extends TorrentStatus

  object Metadata {
    implicit val jsonFormat = jsonFormat2(Metadata.apply)
  }
  case class Metadata(torrentInfo: TorrentInfo, expectedContent:Seq[String])
}

class Torrent private[torrent] (val id: String, info: TorrentInfo, expectedContent:Seq[String], sourceTorrentFile: File, dir: File, getClient: (File, File) => Client) {
  import Torrent._

  val metadata = new FileBackedValue(dir / "metadata.json", Metadata.jsonFormat)
  metadata.set(Metadata(info, expectedContent))

  private val destTorrentFile = dir / "torrent"
  Files.copy(sourceTorrentFile.toPath, destTorrentFile.toPath, StandardCopyOption.REPLACE_EXISTING)

  val client = getClient(destTorrentFile, mkdir(dir / "data"))

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

