name := "snag"

version := "1.0.0-SNAPSHOT"

organization := "org.snag"

scalaVersion := "2.11.8"

resolvers ++= Seq (
  "spray repo" at "http://repo.spray.io",
  "sonatype-oss-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  Resolver.bintrayRepo("galarragas", "maven")
)

libraryDependencies ++= Seq(
  "io.reactivex" %% "rxscala" % "0.26.2",
  "com.turn" % "ttorrent-core" % "1.5",
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1",
  "io.spray" %% "spray-client" % "1.3.3",
  "io.spray" %% "spray-json" % "1.3.2",
  "io.spray" %% "spray-can" % "1.3.3",
  "io.spray" %% "spray-routing" % "1.3.3",
  "com.pragmasoft" %% "spray-funnel" % "1.2-spray1.3",
  "com.typesafe.akka" %% "akka-actor" % "2.4.7",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.7",
  "org.scalawag.timber" %% "timber-backend" % "0.6.0-SNAPSHOT",
  "org.scalawag.timber" %% "slf4j-over-timber" % "0.6.0-SNAPSHOT"
// https://github.com/edmund-wagner/junrar
)

enablePlugins(JavaServerAppPackaging)

