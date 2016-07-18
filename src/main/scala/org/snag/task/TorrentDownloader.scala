package org.snag.task

import akka.actor.ActorSystem
import org.snag.HttpClient
import org.snag.model.{TorrentDownload, TorrentSearch}
import org.snag.service.TorrentDay
import org.snag.model.FileUtils._

class TorrentDownloader[P](search: TorrentSearch[P], torrentDay: TorrentDay)(implicit system: ActorSystem) extends HttpClient {
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
      torrentDay.fetch(chosen, td.dir / "chosen.torrent")
    }
  }
}
