import Dependencies._

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.11.7"
)

lazy val web = (project in file("web")).
  settings(commonSettings: _*).
  settings(
    name := "web",
    libraryDependencies ++= webDeps
  )

lazy val recolector = (project in file("recolector")).
  settings(commonSettings: _*).
  settings(
    name := "recolector",
    libraryDependencies ++= recolectorDeps
  )
