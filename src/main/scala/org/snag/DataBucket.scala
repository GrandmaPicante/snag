package org.snag

import java.io.File
import java.net.InetAddress
import java.util.{Observable, Observer}

import com.turn.ttorrent.client.{Client, SharedTorrent}
import org.snag.model.MediaUniverse
import org.snag.service.{TheMovieDB, TorrentDay}
import org.snag.task.SeriesMetadataFetcher
import org.snag.torrent.Torrenter

import scala.concurrent.ExecutionContext

class DataBucket(val universe: MediaUniverse,
                 val torrenter: Torrenter,
                 val metadataFetcher: SeriesMetadataFetcher,
                 //                 val installer: EpisodeInstaller,
                 val torrentDay: TorrentDay,
                 val theMovieDB: TheMovieDB,
                 val homeDir: File)
