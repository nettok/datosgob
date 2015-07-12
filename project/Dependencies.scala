import sbt._

object Dependencies {
  // Versions
  lazy val akkaVersion = "2.4-M2"

  // Libraries
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % "1.0-RC4"
  val akkaHttpJson = "de.heikoseeberger" %% "akka-http-play-json" % "0.9.1"
  val slick = "com.typesafe.slick" %% "slick" % "3.0.0"
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.3"
  val sqlite = "org.xerial" % "sqlite-jdbc" % "3.8.10.1"
  val selenium = "org.seleniumhq.selenium" % "selenium-java" % "2.46.0"

  // Projects
  val webDeps =
    Seq(akkaActor, akkaHttp, akkaHttpJson)

  val recolectorDeps =
    Seq(selenium, slick, logback, sqlite)
}
