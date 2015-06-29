name := "gc-adjudicaciones-recolector"

version := "0.1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.seleniumhq.selenium" % "selenium-java" % "2.46.0",
  "com.typesafe.slick" %% "slick" % "3.0.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.xerial" % "sqlite-jdbc" % "3.8.10.1"
)
