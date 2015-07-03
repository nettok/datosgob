name := "datosgob-web"

version := "0.1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4-M2",
  "com.typesafe.akka" %% "akka-http-experimental" % "1.0-RC4",
  "de.heikoseeberger" %% "akka-http-play-json" % "0.9.1",
  "com.typesafe.slick" %% "slick" % "3.0.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.xerial" % "sqlite-jdbc" % "3.8.10.1"
)