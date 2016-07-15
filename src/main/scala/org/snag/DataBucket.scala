package org.snag

import java.io.File
import java.net.InetAddress
import java.util.{Observable, Observer}

import com.turn.ttorrent.client.{SharedTorrent, Client}
import org.snag.model.MediaUniverse
import org.snag.service.{TheTVDB, TorrentDay}
import org.snag.task.SeriesMetadataFetcher

import scala.concurrent.ExecutionContext

class DataBucket(val universe: MediaUniverse,
                 val metadataFetcher: SeriesMetadataFetcher,
                 //                 val installer: EpisodeInstaller,
                 val torrentDay: TorrentDay,
                 val thetvdb: TheTVDB)
