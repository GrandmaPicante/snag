package org.snag.model

import java.io.File
import java.util.concurrent.Executors

import akka.dispatch.ExecutionContexts
import rx.lang.scala.Observable
import rx.lang.scala.subjects.ReplaySubject
import spray.json.{ParserInput, JsonParser, RootJsonFormat}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import FileUtils._

object FileBackedValue {
  sealed trait Event[T]
  case class Update[T](t: T) extends Event[T]
}

import FileBackedValue._

class FileBackedValue[T](file: File, jsonFormat: RootJsonFormat[T]) {
  // TODO: ExcecutionContext needs to limit servicing to one thread at a time.  Eventually use a common pool of
  // TODO: threads (with a concurrent limit).
  implicit private[this] val ec = ExecutionContexts.fromExecutorService(Executors.newSingleThreadExecutor)

  private[this] val subject = ReplaySubject[Event[T]]()

  val events: Observable[Event[T]] = subject

  def lastUpdatedAt = file.lastModified()

  private[this] var value: Option[T] =
    if ( file.exists ) {
      val json = JsonParser(ParserInput(Source.fromFile(file).mkString))
      val t = json.convertTo[T](jsonFormat)
      subject.onNext(Update(t))
      Some(t)
    } else {
      None
    }

  def get = Await.result(async.get,Duration.Inf)

  def set(in: T) = Await.result(async.set(in),Duration.Inf)

  // I used Futures to serialize access to the innards of this class.  I could have done it with "synchronized" or locks
  // or anything else, but I decided to leave all this here (even though the class presents a synchronous interface
  // right now) in case we decide to expose the Futures outside the class at some point.  If we decide not to do that,
  // we can get rid of all this Future stuff and just use simple locking. I also could have just used AtomicReference
  // or something since the value is immutable and the set is the only thing that needs to be serialized.  I just left
  // it this way so that it would be consistent with the DirectoryBackedMap.

  object async {
    def get = Future(value)

    def set(in: T) = Future {
      writeFileSafely(file) { w =>
        w.write(jsonFormat.write(in).prettyPrint)
      }

      value = Some(in)

      subject.onNext(Update(in))
    }
  }
}
