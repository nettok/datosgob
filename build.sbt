import Dependencies._

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.11.7"
)

lazy val datos = (project in file("datos")).
  settings(commonSettings: _*).
  settings(
    name := "datos",
    libraryDependencies ++= datosDeps
  )

lazy val web = (project in file("web")).
  dependsOn(datos).
  settings(commonSettings: _*).
  settings(
    name := "web",
    libraryDependencies ++= webDeps
  )

lazy val recolector = (project in file("recolector")).
  dependsOn(datos).
  settings(commonSettings: _*).
  settings(
    name := "recolector",
    libraryDependencies ++= recolectorDeps
  )
