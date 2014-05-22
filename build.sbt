name := "mvn2sbt"

organization := "pl.jozwik"

version := "0.1.0-SNAPSHOTS"

scalaVersion  := "2.11.1"

scalacOptions ++= Seq( "-deprecation", "-unchecked", "-feature")

scalacOptions in Test ++= Seq("-Yrangepos")

libraryDependencies in Global ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.scalacheck" %% "scalacheck" % "1.11.4" % "test",
  "org.scalatest" %% "scalatest" % "2.1.7" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.2"
)

