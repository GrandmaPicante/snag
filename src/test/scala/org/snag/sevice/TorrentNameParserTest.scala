package org.snag.sevice

import org.scalatest.words.ShouldVerb
import org.scalatest.{Matchers, FunSuite}
import org.snag.service.TorrentNameParser._

class TorrentNameParserTest extends FunSuite with ShouldVerb with Matchers {

  private[this] val cases = Seq (
    "American Beauty 1999 1080p BluRay DTS-HD MA 5.1 x264-BluntSlayer" ->
      MovieAttributes(
        title = Some("American Beauty"),
        year = Some(1999),
        group = Some("BluntSlayer"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res1080p),
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = Some(AudioEncoding.DTSHDMA),
        audioChannels = Some(AudioChannels.Surround),
        unrecognizedElements = Nil
      ),
    "American Beauty 1999 720p BluRay x264-LEVERAGE" ->
      MovieAttributes(
        title = Some("American Beauty"),
        year = Some(1999),
        group = Some("LEVERAGE"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res720p),
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = None,
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "American Beauty 1999 BDRip AAC-5 1 x264-AKS74u" ->
      MovieAttributes(
        title = Some("American Beauty"),
        year = Some(1999),
        group = Some("AKS74u"),
        source = Some(Source.BRRip),
        videoResolution = None,
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = Some(AudioEncoding.AAC),
        audioChannels = Some(AudioChannels.Surround),
        unrecognizedElements = Nil
      ),
    "American Beauty 1999 AC3 iNTERNAL DVDRip XviD-xCZ" ->
      MovieAttributes(
        title = Some("American Beauty"),
        year = Some(1999),
        group = Some("xCZ"),
        source = Some(Source.DVDRip),
        videoResolution = None,
        videoEncoding = Some(VideoEncoding.XviD),
        audioEncoding = Some(AudioEncoding.AC3),
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "American Beauty 1999 BRRip XvidHD 720p-NPW" ->
      MovieAttributes(
        title = Some("American Beauty"),
        year = Some(1999),
        group = Some("NPW"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res720p),
        videoEncoding = Some(VideoEncoding.XviDHD),
        audioEncoding = None,
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "The Big Lebowski 1998 1080p BluRay x265 INTERNAL-FLAME" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("FLAME"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res1080p),
        videoEncoding = Some(VideoEncoding.x265),
        audioEncoding = None,
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "The Big Lebowski 1998 BDRip 1080p X265 AC3-D3FiL3R" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("D3FiL3R"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res1080p),
        videoEncoding = Some(VideoEncoding.x265),
        audioEncoding = Some(AudioEncoding.AC3),
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "The Big Lebowski 1998 Internal 1080p BluRay x264-SAiMORNY [NORAR]" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("SAiMORNY"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res1080p),
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = None,
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "The Big Lebowski 1998 BluRay 1080p DTS-HD MA 5 1 x264 dxva-SiMPLE@BluRG" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("SiMPLE@BluRG"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res1080p),
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = Some(AudioEncoding.DTSHDMA),
        audioChannels = Some(AudioChannels.Surround),
        unrecognizedElements = List("dxva")
      ),
    "The Big Lebowski 1998 1080p Bluray DTS-HD X264-BluEvo" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("BluEvo"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res1080p),
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = Some(AudioEncoding.DTSHD),
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "The Big Lebowski 1998 iNTERNAL BDRip x264-EXViDiNT" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("EXViDiNT"),
        source = Some(Source.BRRip),
        videoResolution = None,
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = None,
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "The Big Lebowski 1998 480p HDDVD x264-mSD" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("mSD"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res480p),
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = None,
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "The Big Lebowski 1998 Internal 1080p BluRay x264-SAiMORNY" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("SAiMORNY"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res1080p),
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = None,
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "The Big Lebowski 1998 720p BluRay x264 x0r" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("x0r"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res720p),
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = None,
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "The Big Lebowski 1998 HDRip x264-VLiS" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("VLiS"),
        source = Some(Source.HDTV),
        videoResolution = None,
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = None,
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "The Big Lebowski 1998 720p BDRip x264 AC3-AKS74u" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("AKS74u"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res720p),
        videoEncoding = Some(VideoEncoding.x264),
        audioEncoding = Some(AudioEncoding.AC3),
        audioChannels = None,
        unrecognizedElements = Nil
      ),
    "The Big Lebowski 1998 HDDVD XvidHD 720p-NPW" ->
      MovieAttributes(
        title = Some("The Big Lebowski"),
        year = Some(1998),
        group = Some("NPW"),
        source = Some(Source.BRRip),
        videoResolution = Some(VideoResolution.Res720p),
        videoEncoding = Some(VideoEncoding.XviDHD),
        audioEncoding = None,
        audioChannels = None,
        unrecognizedElements = Nil
      ),

      // This one has a year in the title

      "2001 A Space Odyssey 1968 SUBPACK-EMX" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = Some(1968),
          group = Some("EMX"),
          source = None,
          videoResolution = None,
          videoEncoding = None,
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = List("SUBPACK")
        ),
      "2001 A Space Odyssey 1968 1080p BluRay DTS-HD MA 5 1 X264 DiRTYBURGER" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = Some(1968),
          group = Some("DiRTYBURGER"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res1080p),
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = Some(AudioEncoding.DTSHDMA),
          audioChannels = Some(AudioChannels.Surround),
          unrecognizedElements = Nil
        ),
      "2001 A Space Odyssey 1968 1080p BluRay DTS x264-CyTSuNee" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = Some(1968),
          group = Some("CyTSuNee"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res1080p),
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = Some(AudioEncoding.DTS),
          audioChannels = None,
          unrecognizedElements = Nil
        ),
      "2001 A Space Odyssey 1968 BDRip x264 AC3 RoSubbed-playSD [NO RAR]" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = Some(1968),
          group = Some("playSD"),
          source = Some(Source.BRRip),
          videoResolution = None,
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = Some(AudioEncoding.AC3),
          audioChannels = None,
          unrecognizedElements = List("RoSubbed")
        ),
      "2001 A Space Odyssey 1968 1080p BluRay x264-iNeo" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = Some(1968),
          group = Some("iNeo"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res1080p),
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = Nil
        ),
      "2001 A Space Odyssey 1968 720p BRRIP XVID AC3-MAJESTiC" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = Some(1968),
          group = Some("MAJESTiC"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res720p),
          videoEncoding = Some(VideoEncoding.XviD),
          audioEncoding = Some(AudioEncoding.AC3),
          audioChannels = None,
          unrecognizedElements = Nil
        ),
      "2001 A Space Odyssey 1968 iNTERNAL BDRip x264-MANNEKEPiS" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = Some(1968),
          group = Some("MANNEKEPiS"),
          source = Some(Source.BRRip),
          videoResolution = None,
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = Nil
        ),
      "2001 A Space Odyssey DVDRip x264 AAC-REsuRRecTioN" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = None,
          group = Some("REsuRRecTioN"),
          source = Some(Source.DVDRip),
          videoResolution = None,
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = Some(AudioEncoding.AAC),
          audioChannels = None,
          unrecognizedElements = Nil
        ),
      "2001 A Space Odyssey 1968 480p BluRay x264-mSD" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = Some(1968),
          group = Some("mSD"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res480p),
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = Nil
        ),
      "2001 A Space Odyssey 1968 720p BluRay x264-mSD" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = Some(1968),
          group = Some("mSD"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res720p),
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = Nil
        ),
      "2001 A Space Odyssey 1968 1080p BluRay FLAC VC-1 Remux-decibeL" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = Some(1968),
          group = Some("decibeL"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res1080p),
          videoEncoding = None,
          audioEncoding = Some(AudioEncoding.FLAC),
          audioChannels = None,
          unrecognizedElements = List("VC1Remux")
        ),
      "2001 A Space Odyssey 1968 DVDRip XViD AC3 iNTERNAL-FFM" ->
        MovieAttributes(
          title = Some("2001 A Space Odyssey"),
          year = Some(1968),
          group = Some("FFM"),
          source = Some(Source.DVDRip),
          videoResolution = None,
          videoEncoding = Some(VideoEncoding.XviD),
          audioEncoding = Some(AudioEncoding.AC3),
          audioChannels = None,
          unrecognizedElements = Nil
        ),


      "Fight Club 1999 10th Anniversary Edition 1080p BluRay DTS-HD MA 5.1 x264-BluEvo" ->
        MovieAttributes(
          title = Some("Fight Club"),
          year = Some(1999),
          group = Some("BluEvo"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res1080p),
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = Some(AudioEncoding.DTSHDMA),
          audioChannels = Some(AudioChannels.Surround),
          unrecognizedElements = List("10thAnniversaryEdition")
        ),
      "Fight Club 1999 BDRip x264 AC3 RoSubbed-playSD [NO RAR]" ->
        MovieAttributes(
          title = Some("Fight Club"),
          year = Some(1999),
          group = Some("playSD"),
          source = Some(Source.BRRip),
          videoResolution = None,
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = Some(AudioEncoding.AC3),
          audioChannels = None,
          unrecognizedElements = List("RoSubbed")
        ),
      "Fight Club 1999 COMPLETE Blu-ray AVC DTS-MA 5 1-ChaoS" ->
        MovieAttributes(
          title = Some("Fight Club"),
          year = Some(1999),
          group = Some("ChaoS"),
          source = Some(Source.BRRip),
          videoResolution = None,
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = Some(AudioEncoding.DTSHDMA),
          audioChannels = Some(AudioChannels.Surround),
          unrecognizedElements = List("COMPLETE")
        ),
      "Fight Club 1999 iNTERNAL DVDRip X264-MULTiPLY" ->
        MovieAttributes(
          title = Some("Fight Club"),
          year = Some(1999),
          group = Some("MULTiPLY"),
          source = Some(Source.DVDRip),
          videoResolution = None,
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = Nil
        ),
      "Fight Club 1999 720p BRRip x264 x0r" ->
        MovieAttributes(
          title = Some("Fight Club"),
          year = Some(1999),
          group = Some("x0r"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res720p),
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = Nil
        ),
      "Fight Club 1999 10th Anniversary Edition BDRip 576P X264 AC3-UNiQUE" ->
        MovieAttributes(
          title = Some("Fight Club"),
          year = Some(1999),
          group = Some("UNiQUE"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res576p),
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = Some(AudioEncoding.AC3),
          audioChannels = None,
          unrecognizedElements = List("10thAnniversaryEdition")
        ),
      "Fight Club 1999 REMASTERED REPACK 720p BluRay x264-WLM" ->
        MovieAttributes(
          title = Some("Fight Club"),
          year = Some(1999),
          group = Some("WLM"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res720p),
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = List("REMASTEREDREPACK")
        ),
      "Fight Club 1999 iNTERNAL DVDRip XviD-XviK" ->
        MovieAttributes(
          title = Some("Fight Club"),
          year = Some(1999),
          group = Some("XviK"),
          source = Some(Source.DVDRip),
          videoResolution = None,
          videoEncoding = Some(VideoEncoding.XviD),
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = Nil
        ),
      "Fight Club 1999 1080p 10th Ann Edt BluRay DTS x264 D-Z0N3" ->
        MovieAttributes(
          title = Some("Fight Club"),
          year = Some(1999),
          group = Some("Z0N3"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res1080p),
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = Some(AudioEncoding.DTS),
          audioChannels = None,
          unrecognizedElements = List("10thAnnEdt","D")
        ),
      "Fight Club 1999 10th Anniversary Edition 720p BRRip XviD AC3-FLAWL3SS" ->
        MovieAttributes(
          title = Some("Fight Club"),
          year = Some(1999),
          group = Some("FLAWL3SS"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res720p),
          videoEncoding = Some(VideoEncoding.XviD),
          audioEncoding = Some(AudioEncoding.AC3),
          audioChannels = None,
          unrecognizedElements = List("10thAnniversaryEdition")
        ),

      // Title is a number

      "71 2014 LIMITED 720p BluRay X264-AMIABLE [NO RAR]" ->
        MovieAttributes(
          title = Some("71"),
          year = Some(2014),
          group = Some("AMIABLE"),
          source = Some(Source.BRRip),
          videoResolution = Some(VideoResolution.Res720p),
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = List("LIMITED")
        ),

      // Title contains an apostrophe

      "'71 2014 HDRip x264-KingStoner" ->
        MovieAttributes(
          title = Some("'71"),
          year = Some(2014),
          group = Some("KingStoner"),
          source = Some(Source.HDTV),
          videoResolution = None,
          videoEncoding = Some(VideoEncoding.x264),
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = Nil
        ),

      // Year has crap on it, no group

      "X-Men Origins Wolverine {2009} DVDRIP" ->
        MovieAttributes(
          title = Some("X-Men Origins Wolverine"),
          year = Some(2009),
          group = None,
          source = Some(Source.DVDRip),
          videoResolution = None,
          videoEncoding = None,
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = Nil
        ),

      // No Year, no tags, end up failing to detect the group

      "X-Men Origins Wolverine-RELOADED" ->
        MovieAttributes(
          title = Some("X-Men Origins Wolverine-RELOADED"),
          year = None,
          group = None,
          source = None,
          videoResolution = None,
          videoEncoding = None,
          audioEncoding = None,
          audioChannels = None,
          unrecognizedElements = Nil
        )
  )

  cases foreach { case (txt, attrs) =>
    test(txt) {
      parseMovieTorrentName(txt) shouldBe attrs
    }
  }
/*
  // Numerical title + apostrophe
  71 2014 DVDRip X264 AC3-playSD [NO RAR]
  71 2014 LIMITED 720p BRRIP x264 AC3 OMG
  71 2014 LIMITED 720p BRRiP x264 AC3 SiMPLE
  71 2014 LIMITED 1080p BluRay X264-AMIABLE [NO RAR]
  71 2014 LIMITED 1080p BRRip x264 AAC-m2g
  71 2014 LIMITED 720p BluRay X264-AMIABLE [NO RAR]
  71 2014 LIMITED BRRip XviD AC3-iFT
  71 2014 BRRip XviD AC3-EVO
  71 2014 LIMITED BDRip X264-AMIABLE
  71 2014 1080p HDRip x264 AAC-m2g
  '71 2014 1080p HDRip x264-KingStoner
  '71 2014 HDRip XviD AC3-EVO
  '71 2014 HDRip x264-KingStoner
  // dash in title
  X-Men Origins Wolverine 2009 BluRay 1080p DTS-HD MA 5 1 x264 dxva-SiMPLE@BluRG
  X-Men Origins Wolverine-RELOADED
  X-Men Origins Wolverine 2009 1080p BluRay DTS-HD MA 5.1 x264-BluEvo
  X-Men Origins Wolverine 2009 MULTi COMPLETE BLURAY-USELESS
  X-Men Origins Wolverine 2009 BRRip AAC x264-SSDD
  X-Men Origins Wolverine 2009 BRRip XviD AC3-ViSiON
  X-Men Origins Wolverine BRRip Xvid Ac3 Projekt
  X-Men Origins Wolverine {2009} DVDRIP
  X-Men Origins Wolverine DVDRip XviD-JUAMNJi
*/
}