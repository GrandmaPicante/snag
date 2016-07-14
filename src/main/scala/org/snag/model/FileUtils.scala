package org.snag.model

import java.io.{FileFilter, File, FileWriter, PrintWriter}
import org.snag.Logging.log

object FileUtils {

  def writeFileSafely(f: File)(fn: PrintWriter => Unit): Unit = {
    mkdir(f.getParentFile)

    val tempFile = new File(f.getParentFile,"." + f.getName)
    val pw = new PrintWriter(new FileWriter(tempFile))
    try {
      try {
        fn(pw)
      } finally {
        pw.close()
      }
      if ( ! tempFile.renameTo(f) )
        println(s"failed to rename file $tempFile to $f")
    } catch {
      case ex: Exception =>
        println(s"failed to write file: $tempFile: $ex")
        if ( ! tempFile.delete() )
          println(s"failed to delete file $tempFile")
    }
  }

  def mkdir(dir: File) = {
    if ( dir.exists ) {
      if ( dir.isDirectory )
        log.debug(s"directory $dir already exists")
      else
        throw new IllegalStateException(s"$dir already exists but is not a directory")
    } else {
      if ( dir.mkdirs() )
        log.debug(s"created directory $dir")
      else
        throw new RuntimeException(s"failed to create directory $dir")
    }
    dir
  }

  implicit class FilePimper(f:File) {

    def /(name:String): File = new File(f,name)
    def /(name:Int): File = this / name.toString

    def subdirs =
      if ( f.isDirectory )
        f.listFiles(new FileFilter {
          override def accept(child: File) = child.isDirectory
        }).toList
      else
        throw new IllegalArgumentException("$f is not a directory")

  }
}
