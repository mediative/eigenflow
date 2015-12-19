name := "eigenflow"

organization := "com.mediative"

version := "0.1.0"

scalaVersion := "2.11.7"

scalacOptions ++= Seq(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-encoding", "UTF-8",
  "-Xfatal-warnings",
  "-Xlint",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard"
  )

scalariformSettings


// Dependencies
resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

val akkaVersion = "2.4.1"
val kafkaVersion = "0.9.0.0"

libraryDependencies ++= Seq(
  // akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.github.krasserm" %% "akka-persistence-cassandra" % "0.6",

  // kafka
  "org.apache.kafka" %% "kafka" % kafkaVersion,
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,

  // json
  "com.lihaoyi" %% "upickle" % "0.3.6"
)
