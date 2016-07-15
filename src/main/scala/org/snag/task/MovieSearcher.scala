package org.snag.task

import org.joda.time.{DateTime, Interval}
import org.snag.Logging.log
import org.snag.model.{Movie, TorrentSearch}
import org.snag.service.TorrentDay
import rx.lang.scala.Observable
import rx.lang.scala.subjects.PublishSubject

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object MovieSearcher {
  sealed trait Event
  case class SearchComplete(search: TorrentSearch[Movie]) extends Event
}

import MovieSearcher._

class MovieSearcher(movie: Movie, torrentDay: TorrentDay)(implicit ec: ExecutionContext) {

  private[this] val subject = PublishSubject[Event]
  val events: Observable[Event] = subject


  def search() = {
    // Iterate through the existing searches to see if there's anything that should be resumed.  Otherwise, we'll
    // start a new search from scratch.

    // Decide on some search terms, based on the metadata and configuration.  Create a sequence of searches that should
    // be tried and then execute them in order.  If the terms that we generate already exist in a prior search, don't
    // do it again (or at least wait for some amount of time before doing it again).

    // Minimum time before performing the same search again (and assuming the results may be different)
    val minSearchInterval = 4 hours

    val quality = "720p" // TODO: this should come from the configuration of the movie rather than hard-coded

    val searchQueries = Seq(
      movie.metadata.get.toList flatMap { md =>
        Seq(
          s"${md.title} ${md.yearReleased} $quality",
          md.title
        ) ++ ( md.alternateTitles.toList flatMap { at =>
          Seq(
            s"$at ${md.yearReleased}",
            at
          )
        })
      }
    ).flatten.map(_.replaceAll("""[^\w]+"""," "))

    val now = DateTime.now()

    // Go through each of our search term possibilities and decide on one to execute.
    // First find one that wasn't already recently executed. This means that there isn't a prior search
    // with the same terms and a timestamp that's within the minimum interval.

    val chosenQuery =
      searchQueries find { query =>
        val priorSearches = movie.searches.items.values
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
          val ms = movie.searches.createNext()
          ms.data.set(data)

          subject.onNext(SearchComplete(ms))
        }

      case None =>
        log.debug("no queries that haven't been tried recently were found")
    }

  }

}
