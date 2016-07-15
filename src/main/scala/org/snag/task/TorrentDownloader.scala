package org.snag.task

import java.io.FileOutputStream

import akka.actor.ActorSystem
import org.snag.HttpClient
import org.snag.model.{TorrentDownload, TorrentSearch}
import spray.client.pipelining._
import org.snag.model.FileUtils._
import scala.util.{Failure, Success}
import org.snag.Logging.log

class TorrentDownloader[P](search: TorrentSearch[P])(implicit system: ActorSystem) extends HttpClient {
  protected implicit val actorSystem = system
  private[this] implicit val ec = system.dispatcher

  def go() = {
    // This is dumb and just chooses the first hit.  Eventually, we could put smarts in here that will make it
    // look for the best torrent and choose that one.
    val chosenOption = search.data.get.flatMap(_.hits.headOption)

    // This should be saved even if nothing is selected for diagnostical purposes.
    val td = search.downloads.createNext()
    td.data.set(TorrentDownload.Data(0))

    chosenOption foreach { chosen =>
      // Download the actual torrent file and store it into the directory for this download.
      val torrentUrl = "https://www.torrentday.com/" + chosen.url

      log.debug(s"Downloading $torrentUrl")

      // Need to add auth info here.  Probably move this logic into the TorrentDay service.
      http(Get(torrentUrl)) onComplete {
        case Success(rsp) =>
          // We'll want a way to do this with a stream probably to avoid buffering into memory.
          val torrentContent = rsp.entity.data.toByteArray
          val outs = new FileOutputStream(td.dir / "chosen.torrent")
          try {
            outs.write(torrentContent)
          } finally {
            outs.close()
          }

        case Failure(ex) =>
          log.error(ex)
      }
    }
  }
}
