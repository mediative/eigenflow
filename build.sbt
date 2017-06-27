organization in ThisBuild := "com.mediative"
name in ThisBuild         := "eigenflow"
scalaVersion in ThisBuild := "2.11.11"

Jvm.`1.8`.required

// Dependencies
resolvers += Resolver.bintrayRepo("krasserm", "maven")

val akkaVersion = "2.4.19"
val kafkaVersion = "0.9.0.0"

libraryDependencies ++= Seq(
  // akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.github.krasserm" %% "akka-persistence-cassandra" % "0.6",

  // kafka
  "org.apache.kafka" %% "kafka" % kafkaVersion exclude("log4j", "log4j") exclude("org.slf4j","slf4j-log4j12"),
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,

  // json
  "com.lihaoyi" %% "upickle" % "0.3.6",

  // test libraries
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % "test"
)

enablePlugins(MediativeReleasePlugin, MediativeBintrayPlugin)