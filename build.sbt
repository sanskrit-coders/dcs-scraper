// Incomplete transition from maven to sbt.

name := "dcs-scraper"

scalaVersion := "2.12.3"

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "0.9.29"
  ,"ch.qos.logback" % "logback-core" % "0.9.29"
  , "net.ruippeixotog" % "scala-scraper_2.12" % "2.0.0"
  ,"com.github.sanskrit-coders" % "indic-transliteration_2.12" % "1.9"
  ,"com.github.sanskrit-coders" % "dict-tools_2.12" % "0.1"
  //    ,"com.github.sanskrit-coders" % "sanskrit-lttoolbox" % "0.1"
  //  ,"com.github.sanskrit-coders" % "db-interface" % "1.8"
)

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.4"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

scmInfo := Some(
  ScmInfo(
    url("https://github.com/sanskrit-coders/dcs-scraper"),
    "scm:git@github.com:sanskrit-coders/dcs-scraper.git"
  )
)

assemblyOutputPath in assembly := file("bin/artifacts/dcs-scraper.jar")
mainClass in assembly := Some("stardict_sanskrit.commandInterface")


useGpg := true
publishMavenStyle := true
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  releaseStepCommand("assembly"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)
