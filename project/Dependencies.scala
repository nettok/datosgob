import sbt._

object Dependencies {
  // Versions
  lazy val akkaVersion = "2.4.0"

  // Libraries
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % "1.0"
  val akkaHttpJson = "de.heikoseeberger" %% "akka-http-play-json" % "1.1.0"
  val slick = "com.typesafe.slick" %% "slick" % "3.1.0"
  var slickHikaricp= "com.typesafe.slick" %% "slick-hikaricp" % "3.1.0"
  val hikaricp = "com.zaxxer" % "HikariCP" % "2.3.12"
  val sqlite = "org.xerial" % "sqlite-jdbc" % "3.8.11.1"
  val postgres = "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"
  val selenium = "org.seleniumhq.selenium" % "selenium-java" % "2.48.2"
  val htmlunitdriver = "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.48.2"
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.3"

  // Projects
  val datosDeps =
    Seq(slick, slickHikaricp, hikaricp, postgres, logback)

  val webDeps =
    Seq(akkaActor, akkaHttp, akkaHttpJson)

  val recolectorDeps =
    Seq(selenium, htmlunitdriver, logback)
}
