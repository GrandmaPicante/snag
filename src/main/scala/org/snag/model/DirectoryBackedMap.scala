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

  trait Id[ID] {
    def toFilename(id:ID):String
    def fromFilename(name:String):ID
  }

  trait SequentialId[ID] extends Id[ID] {
    def getNext(values:Iterable[ID]):ID = ???
  }

  implicit object IntId extends Id[Int] with SequentialId[Int] {
    override def toFilename(id: Int): String = id.toString
    override def fromFilename(name: String): Int = name.toInt
    override def getNext(values: Iterable[Int]): Int = {
      (Iterable(0) ++ values).max + 1
    }
  }

  implicit object StringId extends Id[String] {
    override def toFilename(id: String): String = id
    override def fromFilename(name: String): String = name
  }
}

import org.snag.model.DirectoryBackedMap._

class DirectoryBackedMap[ID, T](dir: File)(instantiate: (ID, File) => T)(implicit idType:Id[ID]) {
  mkdir(dir)

  // This allows publication
  private[this] val subject = ReplaySubject[Event[T]]()

  // This is what external listeners should be listening to
  val events: Observable[Event[T]] = subject

  def items: Map[ID, T] = Await.result(async.items,Duration.Inf)

  def get(id: ID): Option[T] = Await.result(async.get(id),Duration.Inf)

  def create(id: ID): T = Await.result(async.create(id),Duration.Inf)

  def create(id: ID, instantiate: (ID, File) => T): T = Await.result(async.create(id, instantiate: (ID, File) => T),Duration.Inf)

  def getOrCreate(id: ID): T = Await.result(async.getOrCreate(id),Duration.Inf)

  def getOrCreate(id: ID, instantiate: (ID, File) => T): T = Await.result(async.getOrCreate(id, instantiate: (ID, File) => T),Duration.Inf)

  def delete(id: ID): Unit = Await.result(async.delete(id),Duration.Inf)

  // I used Futures to serialize access to the innards of this class.  I could have done it with "synchronized" or locks
  // or anything else, but I decided to leave all this here (even though the class presents a synchronous interface
  // right now) in case we decide to expose the Futures outside the class at some point.  If we decide not to do that,
  // we can get rid of all this Future stuff and just use simple locking. I also could have just used AtomicReference
  // or something since the value is immutable and the set is the only thing that needs to be serialized.

  object async {
    // TODO: ExcecutionContext needs to limit servicing to one thread at a time.  Eventually use a common pool of threads (with a concurrent limit).
    implicit private[this] val ec = ExecutionContexts.fromExecutorService(Executors.newSingleThreadExecutor)

    private[this] var value: Map[ID, T] = {
      log.debug(s"Initializing DirectoryBackedMap in $dir")

      dir.listFiles flatMap { f =>
        if ( f.isDirectory ) {
          try {
            val id = idType.fromFilename(f.getName)
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

    def items: Future[Map[ID, T]] = Future(value)

    def get(id: ID): Future[Option[T]] = Future(value.get(id))

    def create(id: ID): Future[T] = Future(doCreate(id, instantiate))

    def create(id: ID, instantiate: (ID, File) => T): Future[T] = Future(doCreate(id, instantiate))

    def getOrCreate(id: ID): Future[T] = Future(value.get(id).getOrElse(doCreate(id, instantiate)))

    def getOrCreate(id: ID, instantiate: (ID, File) => T): Future[T] = Future(value.get(id).getOrElse(doCreate(id, instantiate)))

    def delete(id: ID): Future[Unit] = Future(doDelete(id))

    private[this] def doCreate(id: ID, instantiate: (ID, File) => T): T =
      if ( value.contains(id) )
        throw new IllegalArgumentException(s"collection already contains an item with id $id")
      else {
        val d = dir / idType.toFilename(id)
        mkdir(d)
        val t = instantiate(id, d)
        value += id -> t
        //      subject.onNext(ItemAdded(t))
        subject.onNext(ItemInstantiated(t))
        t
      }

    private[this] def doDelete(id: ID): Unit =
      value.get(id) match {
        case None =>
          throw new IllegalArgumentException(s"no item with id $id")
        case Some(t) =>
          value = value - id
          val subdir = dir / idType.toFilename(id)
          subdir.delete() // TODO: error checking

        //        subject.onNext(ItemRemoved(id, t))
      }
  }
}

class SequentialDirectoryBackedMap[ID, T](dir: File)(instantiate: (ID, File) => T)(implicit idType:SequentialId[ID]) extends DirectoryBackedMap(dir)(instantiate)(idType) {
  def createNext(): T  = create(idType.getNext(items.keys))
  def createNext(instantiate: (ID, File) => T): T  = create(idType.getNext(items.keys), instantiate)
}
