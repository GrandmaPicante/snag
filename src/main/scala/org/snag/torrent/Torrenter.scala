package org.snag.torrent

import java.io.{BufferedInputStream, File, FileInputStream}
import java.net.InetAddress
import java.security.MessageDigest

import com.turn.ttorrent.client.{Client, SharedTorrent}
import org.apache.commons.codec.binary.Hex
import org.snag.model.DirectoryBackedMap
import org.snag.model.FileUtils._
import org.snag.service.TorrentDay.TorrentInfo
import org.snag.torrent.Torrenter.ClientConfig

object Torrenter {
  case class ClientConfig(uploadLimitKbps:Double, downloadLimitKbps:Double)
}

class Torrenter(clientConfig: ClientConfig, dir: File) {
  mkdir(dir)

  private val torrents = new DirectoryBackedMap[String, Torrent](dir)(new Torrent(_, _, getClient(clientConfig)))
  // TODO Shouldn't start all torrents on instantiation.
  // TODO Implement some limit on concurrent downloads.
  getAll.foreach(_.start)

  /**
    * Adds a torrent and begins downloading.
    * @param info TorrentInfo
    * @param expectedContent A Sequence of Strings to store with the Torrent, each of which should identify a
    *                        content item (media file) that is expected to result from successfully downloading
    *                        and extracting the torrent.
    * @param sourceTorrentFile The .torrent file representing this torrent.
    * @return The new Torrent
    */
  def download(info: TorrentInfo, expectedContent:Seq[String], sourceTorrentFile: File):Torrent = {
    val torrentId = {
      val messageDigest = MessageDigest.getInstance("SHA-1")
      val is = new BufferedInputStream(new FileInputStream(sourceTorrentFile))
      try {
        Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).foreach(messageDigest.update)
      } finally {
        is.close
      }
      Hex.encodeHexString(messageDigest.digest)
    }

    val torrent:Torrent = torrents.getOrCreate(torrentId, new Torrent(_:String, _, info, expectedContent, sourceTorrentFile, getClient(clientConfig)))
    torrent.start
    torrent
  }

  /**
    * Gets the torrent represented by the given id, if any.
    * @param id The id
    * @return An optional Torrent
    */
  def get(id:String):Option[Torrent] = {
    torrents.get(id)
  }

  /**
    * Gets all Torrents
    * @return An Iterable of Torrents
    */
  def getAll:Iterable[Torrent] = {
    torrents.items.values
  }

  /**
    * Finds torrents with one or more expected content items that contain the given substring
    * @param substring The substring to search for
    * @return An Iterable of Torrents
    */
  def searchByExpectedContent(substring:String):Iterable[Torrent] = {
    torrents.items.values filter { torrent =>
      torrent.metadata.get exists { metadata =>
        metadata.expectedContent.exists(_.contains(substring))
      }
    }
  }

  private[this] def getClient(clientConfig: ClientConfig)(torrentFile: File, dataDirectory: File):Client = {
    val client = new Client(InetAddress.getByAddress(Array[Byte](0,0,0,0)), SharedTorrent.fromFile(torrentFile, dataDirectory))
    client.setMaxUploadRate(clientConfig.uploadLimitKbps)
    client.setMaxDownloadRate(clientConfig.downloadLimitKbps)
    client
  }
}
