package org.snag.task

import org.snag.Logging.log
import org.joda.time.{Interval, DateTime}
import org.snag.model.{TorrentSearch, Episode}
import org.snag.service.TorrentDay
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class EpisodeSearcher(episode: Episode, torrentDay: TorrentDay)(implicit ec: ExecutionContext) {

  def search() = {
    // Iterate through the existing searches to see if there's anything that should be resumed.  Otherwise, we'll
    // start a new search from scratch.

    // Decide on some search terms, based on the metadata and configuration.  Create a sequence of searches that should
    // be tried and then execute them in order.  If the terms that we generate already exist in a prior search, don't
    // do it again (or at least wait for some amount of time before doing it again).

    // Minimum time before performing the same search again (and assuming the results may be different)
    val minSearchInterval = 4 hours

    val searchQueries = Seq(
      for {
        series <- episode.season.series.metadata.get
      } yield {
        s"%s S%02dE%02d".format(series.title, episode.season.id, episode.id)
      }
    ).flatten.map(_.replaceAll("""[^\w]+"""," "))

    val now = DateTime.now()

    // Go through each of our search term possibilities and decide on one to execute.
    // First find one that wasn't already recently executed. This means that there isn't a prior search
    // with the same terms and a timestamp that's within the minimum interval.

    val chosenQuery =
      searchQueries find { query =>
        val priorSearches = episode.searches.items.values
        val priorSearchesSameTerms = priorSearches filter { ps =>
          ps.data.get.map(_.query) == Some(query)
        }

        if ( priorSearches.isEmpty ) {
          true
        } else {
          implicit val ordering = new Ordering[DateTime] {
            override def compare(x: DateTime, y: DateTime) = x.getMillis.compareTo(y.getMillis)
          }
          val mostRecentTimestamp = priorSearchesSameTerms.flatMap(_.data.get.map(_.timestamp)).max
          val age = new Interval(mostRecentTimestamp,now).toDuration.getMillis
          log.debug(s"most recent search was performed at $mostRecentTimestamp ($age ms ago)")
          age > minSearchInterval.toMillis
        }
      }

    chosenQuery match {
      case Some(q) =>
        log.debug(s"searching TorrentDay for: $q")
        torrentDay.search(q) map { hits =>
          val hitList = hits.toList
          log.debug(s"TorrentDay returned ${hitList.size} hits")
          val data = TorrentSearch.Data(q, now, hitList)
          val es = episode.searches.createNext()
          es.data.set(data)
        }

      case None =>
        log.debug("no queries that haven't been tried recently were found")
    }

  }

}
