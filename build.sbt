organization in ThisBuild := "com.mediative"
name in ThisBuild         := "eigenflow"
scalaVersion in ThisBuild := "2.11.7"

Jvm.`1.8`.required

// Dependencies
resolvers += Resolver.bintrayRepo("krasserm", "maven")

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
  "com.lihaoyi" %% "upickle" % "0.3.6",

  // test libraries
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
)

enablePlugins(MediativeReleasePlugin, MediativeBintrayPlugin)