package org.snag.service

import java.time.LocalDate
import scala.annotation.tailrec
import scala.reflect._
import scala.collection.JavaConversions._

object TorrentNameParser {

  sealed trait Attribute

  sealed trait Source extends Attribute

  object Source {
    case object Cam extends Source
    case object Telesync extends Source
    case object Workprint extends Source
    case object Telecine extends Source
    case object PayPerView extends Source
    case object Screener extends Source
    case object DDC extends Source
    case object R5 extends Source
    case object DVDRip extends Source
    case object DVDR extends Source
    case object HDTV extends Source
    case object PDTV extends Source
    case object DSRip extends Source
    case object VODRip extends Source
    case object WEBDL	extends Source
    case object WEBRip extends Source
    case object WEBCap extends Source
    case object BRRip extends Source
  }

  sealed trait VideoResolution extends Attribute

  object VideoResolution {
    case object Res480p extends VideoResolution
    case object Res576p extends VideoResolution
    case object Res720p extends VideoResolution
    case object Res1080p extends VideoResolution
  }

  sealed trait VideoEncoding extends Attribute

  object VideoEncoding {
    case object x264 extends VideoEncoding
    case object x265 extends VideoEncoding
    case object XviD extends VideoEncoding
    case object XviDHD extends VideoEncoding
  }

  sealed trait AudioEncoding extends Attribute

  object AudioEncoding {
    case object AAC extends AudioEncoding
    case object AC3 extends AudioEncoding
    case object DTS extends AudioEncoding
    case object DTSHD extends AudioEncoding
    case object DTSHDMA extends AudioEncoding
    case object FLAC extends AudioEncoding
  }

  sealed trait AudioChannels extends Attribute

  object AudioChannels {
    case object Stereo extends AudioChannels
    case object Surround extends AudioChannels
  }

  sealed trait Ignored extends Attribute

  // These create attributes but aren't actually used.  It's basically a set of uninteresting but known tokens.
  object Ignored {
    case object Internal extends Ignored
  }

  // This is magic attribute used by the parser to mark what it thinks is the group name.  It changes for every
  // torrent name parsed, unlike the rest of the attributes above.
  private object GroupName extends Attribute

  object Attribute {

    val registry = {

      def mappings(attr: Attribute, tags: String*) = {
        tags.map(_.toLowerCase -> attr)
      }

      List(
        mappings(Source.Cam,"Cam","CAMRip"),
        mappings(Source.Telesync,"TS","TELESYNC","PDVD"),
        mappings(Source.Workprint,"WP","WORKPRINT"),
        mappings(Source.Telecine,"TC","TELECINE"),
        mappings(Source.PayPerView,"PPV","PPVRip"),
        mappings(Source.Screener,"SCR","SCREENER","DVDSCR","DVDSCREENER","BDSCR"),
        mappings(Source.DDC,"DDC"),
        mappings(Source.R5,"R5"),
        mappings(Source.DVDRip,"DVDRip"),
        mappings(Source.DVDR,"DVDR","DVDFull","FullRip","ISO","DVD5","DVD9"),
        mappings(Source.HDTV,"HDTV","HDTVRip","HDRip","TVRip"),
        mappings(Source.PDTV,"PDTV"),
        mappings(Source.DSRip,"DSR","SATRip","DTHRip","DVBRip"),
        mappings(Source.VODRip,"VODRip","VODR"),
        mappings(Source.WEBDL,"WEBDL","WEB"),
        mappings(Source.WEBRip,"WEBRip"),
        mappings(Source.WEBCap,"WEBCap"),
        mappings(Source.BRRip,"BRRip","BD","BDRip","BluRay","BDR","BD5","BD9","HDDVD"),

        mappings(VideoResolution.Res480p,"480p"),
        mappings(VideoResolution.Res576p,"576p"),
        mappings(VideoResolution.Res720p,"720p"),
        mappings(VideoResolution.Res1080p,"1080p"),

        mappings(VideoEncoding.x264,"x264","AVC"),
        mappings(VideoEncoding.x265,"x265","HEVC"),
        mappings(VideoEncoding.XviD,"XviD"),
        mappings(VideoEncoding.XviDHD,"XviDHD"),

        mappings(AudioEncoding.AC3,"AC3"),
        mappings(AudioEncoding.AAC,"AAC"),
        mappings(AudioEncoding.DTS,"DTS"),
        mappings(AudioEncoding.DTSHD,"DTSHD"),
        mappings(AudioEncoding.DTSHDMA,"DTSHDMA","DTSMA"),
        mappings(AudioEncoding.FLAC,"FLAC"),

        mappings(AudioChannels.Stereo,"Stereo","2ch"),
        mappings(AudioChannels.Surround,"Surround","51","6ch"),

        mappings(Ignored.Internal,"INTERNAL")
      ).flatten.toMap
    }

    def lookup(tag: String) = {
      registry.get(tag.toLowerCase)
    }
  }

  trait TitleTags {
    val group: Option[String]
    val source: Option[Source]
    val videoResolution: Option[VideoResolution]
    val videoEncoding: Option[VideoEncoding]
    val audioEncoding: Option[AudioEncoding]
  }

  case class MovieAttributes(title: Option[String] = None,
                             year: Option[Int] = None,
                             group: Option[String] = None,
                             source: Option[Source] = None,
                             videoResolution: Option[VideoResolution] = None,
                             videoEncoding: Option[VideoEncoding] = None,
                             audioEncoding: Option[AudioEncoding] = None,
                             audioChannels: Option[AudioChannels] = None,
                             unrecognizedElements: List[String] = Nil) extends TitleTags


  // This is a regular expression that will match any of our tags set off by word boundaries.  It's used to find the
  // first tag in a raw name string.

  private[this] val tagWordRegExp = Attribute.registry.keySet.mkString("(?i)\\b(","|",")\\b").r

  private[this] val trimRightRegExp = """[\W]*$""".r
  private[this] def trimRight(s: String) = trimRightRegExp.replaceFirstIn(s,"")

  private[this] val trimLeftRegExp = """^[\W]*""".r
  private[this] def trimLeft(s: String) = trimLeftRegExp.replaceFirstIn(s,"")

  def parseMovieTorrentName(name: String) = {
    // Expected pattern is {title} "{year} {attributes} {group} {no rar}" where some parts may be missing

    // Before we start looking for anything else, get rid of the optional [NORAR] at the end.  We don't use it for
    // anything anyway, so getting it out the way early will be nice.

    val nameWithoutNoRar = name.mkString.replaceFirst("\\s*\\[NO\\s*RAR\\]$","")

    // Before we start looking for attributes, try to guess the group name.  This is important because the group often
    // contains some of the tags as a substring ("divx", "ts", etc.).  The convention seems to be to either include
    // it after a dash in the last token or for it to be the last token (if there's no dash).

    val groupCandidate = {
      val candidate = nameWithoutNoRar.replaceAll(".*[-\\s]","")

      // It's not really a group name candidate if it's one of the attribute tags above.  In that case, ignore it.

      if ( Attribute.lookup(candidate.toLowerCase).isDefined )
        None
      else
        Some(candidate)
    }

    // Mix this candidate group name into the list of tags we're looking for (in the appropriate place, given its
    // length), so that we can detect it when we run into it, but we won't prefer it to parts of a longer tag.
    // Consider the case of "DTS-HD" to understand why.

    val allTagsInOrder = {
      val allTags = TorrentNameParser.Attribute.registry.toList ++ groupCandidate.map( _.toLowerCase -> GroupName ).toList
      // Force it into a lazy stream to optimize the code below
      allTags.sortBy(_._1.length).reverse.toStream
    }

    // Next, look for the year.  It's the most recognizable thing with no prior knowledge.  That will be the separator
    // between the title and the attributes/group name. If there are multiple years, assume that the last one is the
    // actual year and the prior one(s) are part of the title. Only consider years that are not part of a larger word
    // to prevent false positives on groups with years in them.  Also, only consider years that could actually be
    // release dates and not just any old year (like 1066).

    val (title, year, rawTags) = {
      val re = """\b\d{4}\b""".r
      val lastMatch = re.findAllIn(nameWithoutNoRar).matchData.toList filter { rm =>
        val i = rm.toString.toInt
        i > 1900 && i < LocalDate.now.getYear && rm.start > 0
      } lastOption

      lastMatch match {

        case Some(rm) =>
          // If we found a year, then we can easily derive the title (everything before that) and the tags (everything
          // after that.

          val title = Some(trimRight(nameWithoutNoRar.substring(0,rm.start)))
          val year = Some(rm.toString.toInt)
          val tags = Some(trimLeft(nameWithoutNoRar.substring(rm.end)))

          (title, year, tags)

        case None =>
          // If there's no year, the best we can do is look for the first thing that we consider a tag and
          // assume that everything before that is the title. In this case, we'll pay attention to punctuation
          // because we don't want to see something like 'ts' in a title (pretty common) and assume that it's a tag.
          // For this, the tags have to be set off (not part of a larger word).

          val firstTagMatch = tagWordRegExp.findAllIn(nameWithoutNoRar).matchData.toList.headOption


          firstTagMatch match {
            case Some(rm) =>
              val title = Some(trimRight(nameWithoutNoRar.substring(0,rm.start)))
              val tags = Some(trimLeft(nameWithoutNoRar.substring(rm.start)))
              (title, None, tags)

            case None =>
              (Some(nameWithoutNoRar), None, None)
          }
      }
    }

    // Look for tags by slamming them all together and then looking for the longest tags first and extracting what
    // we find.  This combats the problem of tags like "51" v. "5 1" vs "5.1" and "WEB-DL" v. "WEBDL" v. "WEB DL"
    // where the punctuation and spacing aren't important.  It also keeps us from prematurely grabbing "DTS" when
    // "DTS-HD MA" is the real tag and all that stuff is yet to be seen (left to right).

    @tailrec
    def extract(todo: List[String], remains: List[String] = Nil, attributes: Set[Attribute] = Set.empty): (List[String], Set[Attribute]) = {
      if ( todo.isEmpty )
        (remains, attributes)
      else if ( todo.head.isEmpty )
        extract(todo.tail, remains, attributes)
      else {
        // Look for the tags starting with the longest and working our way down.  When we find one, split the
        // remainder of the to-do string and continue.
        val hit =
          allTagsInOrder map { case (tag,attr) =>
            (tag,attr,todo.head.toLowerCase.indexOf(tag))
          } find { case(tag, attr, pos) =>
            pos >= 0
          }

        hit match {
          case None =>
            // No tags found.  Give up on this string and place it into "remains."
            extract(todo.tail, remains :+ todo.head, attributes)
          case Some((tag, attr, pos)) =>
            // Found a tag.  Add the corresponding attribute and break up the to-do string for the next call.
            extract(todo.head.take(pos) :: todo.head.drop(pos + tag.length) :: todo.tail, remains, attributes + attr)
        }
      }
    }

    val (remains,attributes) =
      rawTags match {
        case None =>
          (Nil, Nil)
        case Some(tags) =>
          extract(List(tags.replaceAll("[ .-]","")))
    }

    def selectFirst[T <: Attribute : ClassTag](attrs: Iterable[Attribute]): Option[T] = {
      val targetClass = classTag[T].runtimeClass
      val all = attributes.filter( c => targetClass.isAssignableFrom(c.getClass) )
      if ( all.isEmpty ) {
        None
      } else if ( all.size == 1 ) {
        Some(all.head.asInstanceOf[T])
      } else {
        throw new RuntimeException(s"multiple mutually-exclusive attributes detected: $all")
        Some(all.head.asInstanceOf[T])
      }
    }

    val groupName =
      if ( attributes contains GroupName )
        groupCandidate
      else
        None

    MovieAttributes(title, year, groupName,
                    selectFirst[Source](attributes),
                    selectFirst[VideoResolution](attributes),
                    selectFirst[VideoEncoding](attributes),
                    selectFirst[AudioEncoding](attributes),
                    selectFirst[AudioChannels](attributes),
                    remains)
  }

}
