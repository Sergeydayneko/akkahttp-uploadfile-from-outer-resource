name := "ru.dayneko"

version := "0.1"

scalaVersion := "2.12.6"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"

libraryDependencies ++= Seq(
  "de.heikoseeberger" %% "akka-http-circe" % "1.17.0",
  "com.typesafe.akka" %% "akka-actor" % "2.5.13",
  "com.typesafe.akka" %% "akka-stream" % "2.5.13",
  "com.typesafe.akka" %% "akka-http" % "10.1.3",
  "com.typesafe.akka" %% "akka-http-xml" % "10.1.3",
  "io.spray" %%  "spray-json" % "1.3.4",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.3",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.jason-goodwin" %% "authentikat-jwt" % "0.4.5",
  "io.circe" %% "circe-core" % "0.9.3",
  "io.circe" %% "circe-generic" % "0.9.3",
  "io.circe" %% "circe-parser" % "0.9.3",
  "com.softwaremill.akka-http-session" %% "core" % "0.5.5",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.3" % Test
)