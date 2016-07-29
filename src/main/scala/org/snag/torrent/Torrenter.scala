package org.snag.torrent

import java.io.{BufferedInputStream, File, FileInputStream}
import java.net.InetAddress
import java.security.MessageDigest

import com.turn.ttorrent.client.{Client, SharedTorrent}
import org.apache.commons.codec.binary.Hex
import org.snag.model.FileUtils._
import org.snag.service.TorrentDay.TorrentInfo
import org.snag.torrent.Torrenter.ClientConfig

object Torrenter {
  case class ClientConfig(uploadLimitKbps:Double, downloadLimitKbps:Double)
}

class Torrenter(clientConfig: ClientConfig, dir: File) {
  mkdir(dir)

  def download(info: TorrentInfo, sourceTorrentFile: File):Torrent = {
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

    val torrent = new Torrent(torrentId, info, sourceTorrentFile, dir / torrentId, getClient(clientConfig))
    torrent.start
    torrent
  }

  private[this] def getClient(clientConfig: ClientConfig)(torrentFile: File, dataDirectory: File):Client = {
    val client = new Client(InetAddress.getByAddress(Array[Byte](0,0,0,0)), SharedTorrent.fromFile(torrentFile, dataDirectory))
    client.setMaxUploadRate(clientConfig.uploadLimitKbps)
    client.setMaxDownloadRate(clientConfig.downloadLimitKbps)
    client
  }
}
