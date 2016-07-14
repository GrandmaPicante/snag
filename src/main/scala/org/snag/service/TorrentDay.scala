package org.snag.service

import java.io.ByteArrayInputStream

import akka.actor.ActorSystem
import com.sun.org.apache.xalan.internal.xsltc.trax.SAX2DOM
import org.ccil.cowan.tagsoup.Parser
import org.snag.{Configuration, HttpClient}
import org.w3c.dom.{Document, Element, Node, NodeList}
import org.xml.sax.InputSource
import spray.client.pipelining._
import spray.http._

import scala.concurrent.Future

object TorrentDay {
  case class TorrentInfo(id: Long, title: String, url: String, sizeKilobytes: Int, ageMinutes: Int, seeders: Int, leechers: Int)
}

import org.snag.service.TorrentDay._

class TorrentDay(cfg: Configuration.TorrentDay)(implicit ac: ActorSystem) extends HttpClient {
  override val actorSystem = ac
  implicit private val ec = actorSystem.dispatcher

  def search(query: String): Future[Iterable[TorrentInfo]] = {
    val uri = Uri("https://www.torrentday.com/browse.php").copy(query = Uri.Query("search" -> query))
    val response: Future[HttpResponse] = http(
      Get(uri).withHeaders(
        HttpHeaders.Cookie(HttpCookie("uid",cfg.uid),HttpCookie("pass",cfg.pass))
      ))

    response map { rsp =>
      val in = new ByteArrayInputStream(rsp.entity.data.toByteArray)
      val doc =
        try {
          val parser = new org.ccil.cowan.tagsoup.Parser

          parser.setFeature(Parser.namespacesFeature, false)
          parser.setFeature(Parser.namespacePrefixesFeature, false)
          val sax2dom = new SAX2DOM(false)
          parser.setContentHandler(sax2dom)
          parser.parse(new InputSource(in))
          sax2dom.getDOM().asInstanceOf[Document]
        } finally {
          in.close()
        }

      // Find torrentTable and get all the rows in it to find the torrents

      val torrentTable = doc.getElementById("torrentTable")
      val torrentInfos =
        torrentTable.getElementsByTagName("tr").drop(1) map { tr =>
          val tds = tr.asInstanceOf[Element].getElementsByTagName("td").toList.map(_.asInstanceOf[Element])

          val tags = tds(1).getTextContent
          val age = reAge.findFirstIn(tags)

//            tds.zipWithIndex foreach { case (td,n) =>
//              println(s"$n = ${td.getTextContent}")
//            }
//            println(s"TAGS: $tags")
//            println(s"AGE: $age")

          TorrentInfo(
            tds(2).getElementsByTagName("a").item(0).asInstanceOf[Element].getAttribute("href").split('/')(1).toLong,
            tds(1).getElementsByTagName("a").item(0).getTextContent,
            tds(2).getElementsByTagName("a").item(0).asInstanceOf[Element].getAttribute("href"),
            normalizeSize(tds(4).getTextContent).toInt,
            age.map(normalizeAge).map(_.toInt).getOrElse(0),
            tds(5).getTextContent.toInt,
            tds(6).getTextContent.toInt

          )
        }


      torrentInfos
    }
  }

  private val reAge = """\d\.\d \w+ ago""".r

  implicit def convertNodeList(nl: NodeList): Iterable[Node] = new Iterable[Node] {
    override def iterator = new Iterator[Node] {
      private var current = 0

      override def hasNext = current < nl.getLength

      override def next(): Node = {
        current += 1
        nl.item(current - 1)
      }
    }
  }

  private def normalizeSize(text: String) = {
    val parts = text.split(' ')
    val num =  parts(0).toFloat
    val units = parts(1)
    if ( units == "MB" )
      num * 1000
    else if ( units == "GB" )
      num * 1000000
    else if ( units == "TB" )
      num * 1000000000
    else
      throw new IllegalArgumentException(s"wasn't expecting unit: $units")
  }

  private def normalizeAge(text: String) = {
    val parts = text.split(' ')
    val num =  parts(0).toFloat
    val units = parts(1)
    if ( units == "minutes" )
      num
    else if ( units == "hours" )
      num * 60
    else if ( units == "days" )
      num * 60 * 24
    else if ( units == "weeks" )
      num * 60 * 24 * 7
    else if ( units == "months" )
      num * 60 * 24 * 30
    else if ( units == "years" )
      num * 60 * 24 * 365
    else
      throw new IllegalArgumentException(s"wasn't expecting unit: $units")
  }
}
