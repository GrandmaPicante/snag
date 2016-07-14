package org.snag.model

import java.io.File
import java.util.concurrent.Executors
import FileUtils._
import akka.dispatch.ExecutionContexts
import org.snag.Logging.log
import rx.lang.scala.Observable
import rx.lang.scala.subjects.ReplaySubject
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object DirectoryBackedMap {
  sealed trait Event[T]
  case class ItemInstantiated[T](item: T) extends Event[T]
//  case class ItemAdded[T](item: T) extends Event[T]
}

import org.snag.model.DirectoryBackedMap._

class DirectoryBackedMap[T](dir: File)(instantiate: (Int, File) => T) {
  mkdir(dir)

  // This allows publication
  private[this] val subject = ReplaySubject[Event[T]]()

  // This is what external listeners should be listening to
  val events: Observable[Event[T]] = subject

  def items: Map[Int, T] = Await.result(async.items,Duration.Inf)

  def get(id: Int): Option[T] = Await.result(async.get(id),Duration.Inf)

  def create(id: Int): T = Await.result(async.create(id),Duration.Inf)

  def createNext(): T = Await.result(async.createNext(),Duration.Inf)

  def getOrCreate(id: Int): T = Await.result(async.getOrCreate(id),Duration.Inf)

  def delete(id: Int): Unit = Await.result(async.delete(id),Duration.Inf)

  // I used Futures to serialize access to the innards of this class.  I could have done it with "synchronized" or locks
  // or anything else, but I decided to leave all this here (even though the class presents a synchronous interface
  // right now) in case we decide to expose the Futures outside the class at some point.  If we decide not to do that,
  // we can get rid of all this Future stuff and just use simple locking. I also could have just used AtomicReference
  // or something since the value is immutable and the set is the only thing that needs to be serialized.

  object async {
    // TODO: ExcecutionContext needs to limit servicing to one thread at a time.  Eventually use a common pool of threads (with a concurrent limit).
    implicit private[this] val ec = ExecutionContexts.fromExecutorService(Executors.newSingleThreadExecutor)

    private[this] var value: Map[Int, T] = {
      log.debug(s"Initializing DirectoryBackedMap in $dir")

      dir.listFiles flatMap { f =>
        if ( f.isDirectory ) {
          try {
            val id = f.getName.toInt
            val t = instantiate(id, f)
            subject.onNext(ItemInstantiated(t))
            log.debug(s"found item directory: ${f.getName}, created $t")
            Some(id -> t)

          } catch {
            case ex: NumberFormatException =>
              log.debug(s"ignoring non-numerical directory: ${f.getName}")
              None
          }
        } else {
          log.debug(s"ignoring plain-file (non-directory): ${f.getName}")
          None
        }
      } toMap
    }

    def items: Future[Map[Int, T]] = Future(value)

    def get(id: Int): Future[Option[T]] = Future(value.get(id))

    def create(id: Int): Future[T] = Future(doCreate(id))

    def createNext(): Future[T] = Future(doCreate(( Iterable(0) ++ value.keys).max + 1))

    def getOrCreate(id: Int): Future[T] = Future(value.get(id).getOrElse(doCreate(id)))

    def delete(id: Int): Future[Unit] = Future(doDelete(id))

    private[this] def doCreate(id: Int): T =
      if ( value.contains(id) )
        throw new IllegalArgumentException(s"collection already contains an item with id $id")
      else {
        val d = dir / id
        mkdir(d)
        val t = instantiate(id, d)
        value += id -> t
        //      subject.onNext(ItemAdded(t))
        subject.onNext(ItemInstantiated(t))
        t
      }

    private[this] def doDelete(id: Int): Unit =
      value.get(id) match {
        case None =>
          throw new IllegalArgumentException(s"no item with id $id")
        case Some(t) =>
          value = value - id
          val subdir = dir / id
          subdir.delete() // TODO: error checking

        //        subject.onNext(ItemRemoved(id, t))
      }
  }
}
