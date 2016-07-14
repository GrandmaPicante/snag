package org.snag

import java.io.File
import java.net.InetAddress
import java.util.{Observable, Observer}

import com.turn.ttorrent.client.{SharedTorrent, Client}
import org.snag.model.MediaUniverse
import org.snag.service.TorrentDay
import org.snag.task.MetadataFetcher
import org.snag.service.thetvdb.TheTVDB

import scala.concurrent.ExecutionContext

class DataBucket(val universe: MediaUniverse,
                 val metadataFetcher: MetadataFetcher,
//                 val installer: EpisodeInstaller,
                 val torrentDay: TorrentDay,
                 val thetvdb: TheTVDB)
