package org.snag.model

import java.io.File
import FileUtils._
import spray.json.DefaultJsonProtocol._

object TorrentDownload {

  case class Config(terms: String)

  object Config {
    implicit val jsonFormat = jsonFormat1(Config.apply)
  }

}

import TorrentDownload._

class TorrentDownload[PARENT](parent: PARENT, id: Int, dir: File) {
  private[this] val myConfig = new FileBackedValue(dir / "config.json", Config.jsonFormat)

  def config = myConfig.get
  def config_=(in: Config) = myConfig.set(in)
}
