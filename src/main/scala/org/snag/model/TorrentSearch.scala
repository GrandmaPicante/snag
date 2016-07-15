package org.snag.model

import java.io.File
import FileUtils._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.snag.service.TorrentDay.TorrentInfo
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

object TorrentSearch {

  case class Data(query: String, timestamp: DateTime, hits: List[TorrentInfo])

  object Data {
    implicit object DateTimeFormat extends RootJsonFormat[DateTime] {
      private[this] val fmt = ISODateTimeFormat.dateTime()
      override def write(t: DateTime): JsValue = JsString(fmt.print(t))
      override def read(json: JsValue): DateTime = json match {
        case JsString(s) => fmt.parseDateTime(s)
        case x => throw new IllegalArgumentException(s"invalid DateTime: $x")
      }
    }
    implicit val torrentInfoFormat = jsonFormat7(TorrentInfo.apply)
    implicit val jsonFormat = jsonFormat3(Data.apply)
  }

}

import TorrentSearch._

class TorrentSearch[PARENT](parent: PARENT, id: Int, dir: File) {
  val data = new FileBackedValue[Data](dir / "data.json", Data.jsonFormat)
}
