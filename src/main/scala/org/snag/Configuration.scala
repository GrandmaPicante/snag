package org.snag

import java.io.File
import org.snag.model.FileUtils._
import spray.json.DefaultJsonProtocol._
import spray.json.{ParserInput, JsonParser}
import scala.io.Source

object Configuration {
  case class TorrentDay(uid: String, pass: String)
  case class TheTVDB(userKey: String, apiKey: String)
  case class TheMovieDB(apiKey: String)

  implicit private val TorrentDayFormat = jsonFormat2(TorrentDay)
  implicit private val TheTVDBFormat = jsonFormat2(TheTVDB)
  implicit private val theMovieDBFormat = jsonFormat1(TheMovieDB)
  private val ConfigurationFormat = jsonFormat3(Configuration.apply)

  def load(f: File): Configuration =
    JsonParser(ParserInput(Source.fromFile(f).mkString)).convertTo[Configuration](ConfigurationFormat)
}

import org.snag.Configuration._

case class Configuration(torrentDay: TorrentDay,
                         theTvDb: TheTVDB,
                         theMovieDb: TheMovieDB)
{
  def save(f: File): Unit =
    writeFileSafely(f) { w =>
      w.write(Configuration.ConfigurationFormat.write(this).prettyPrint)
    }
}
