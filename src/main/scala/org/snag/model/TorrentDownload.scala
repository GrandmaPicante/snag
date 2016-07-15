package org.snag.model

import java.io.File
import FileUtils._
import spray.json.DefaultJsonProtocol._

object TorrentDownload {
  // Eventually, this could contain data about why this particular torrent was chosen from all of those available
  // after the search.  This will allow for debugging.
  case class Data(chosenTorrentIndex: Int)

  object Data {
    implicit val jsonFormat = jsonFormat1(Data.apply)
  }
}

import TorrentDownload._

class TorrentDownload[PARENT](parent: PARENT, id: Int, val dir: File) {
  val data = new FileBackedValue(dir / "config.json", Data.jsonFormat)
}
