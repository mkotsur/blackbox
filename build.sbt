ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "3.1.1"
ThisBuild / organization := "nl.absolutevalue"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation", // emit warning and location for usages of deprecated APIs
  "-explain", // explain errors in more detail
  "-explain-types", // explain type errors in more detail
  "-feature", // emit warning and location for usages of features that should be imported explicitly
  "-indent", // allow significant indentation.
  "-new-syntax", // require `then` and `do` in control expressions.
  "-print-lines", // show source code line numbers.
  "-unchecked", // enable additional warnings where generated code depends on assumptions
  "-Ykind-projector", // allow `*` as wildcard to be compatible with kind projector
  "-Xfatal-warnings", // fail the compilation if there are any warnings
  "-Xmigration", // warn about constructs whose behavior may have changed since version
  "-source:3.1"
)

lazy val runner = project
  .in(file("runner"))
  .settings(
    name := "Runner",
    libraryDependencies ++= Seq(
      deps.dockerJava,
      deps.dockerJavaTransport,
      deps.log4cats,
      deps.slf4jSimple,
      deps.FS2,
      deps.catsEffect,
      deps.test.scalaTest,
      deps.test.catsEffectTesting
    )
  )

lazy val restApi = project
  .in(file("rest-api"))
  .settings(
    name := "REST API",
    libraryDependencies ++= Seq(
      deps.log4cats,
      deps.slf4jSimple,
      deps.FS2,
      deps.catsEffect,
      deps.tapir,
      deps.test.scalaTest,
      deps.test.catsEffectTesting
    ) ++ deps.http4sServer
  )
  .dependsOn(runner)

val deps = new {

  lazy val V = new {
    val http4s = "0.23.11"
    val circe = "0.15.0-M1"
  }

  val dockerJava = "com.github.docker-java" % "docker-java" % "3.2.13"
  val dockerJavaTransport = "com.github.docker-java" % "docker-java-transport-zerodep" % "3.2.13"
  val FS2 = "co.fs2" %% "fs2-core" % "3.2.5"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.3.7"
  val log4cats = "org.typelevel" %% "log4cats-slf4j" % "2.2.0"
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % "2.0.0-alpha6"

  val tapir = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "1.0.0-M2"

  val http4sServer =
    Seq(
      "org.http4s" %% "http4s-dsl" % V.http4s,
      "org.http4s" %% "http4s-blaze-server" % V.http4s,
      "org.http4s" %% "http4s-circe" % V.http4s,
      "io.circe" %% "circe-generic" % V.circe
    )

  val test = new {
    val scalaTest = "org.scalatest" %% "scalatest" % "3.2.11" % "test"
    val catsEffectTesting = "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0"
  }
}
