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

lazy val util = project
  .in(file("util"))
  .settings(
    name := "Utilities",
    libraryDependencies ++= Seq(
      deps.commonsIO,
      deps.catsEffect,
      deps.test.scalaTest,
      deps.test.catsEffectTesting
    ) ++ deps.logging
  )

lazy val runner = project
  .in(file("runner"))
  .settings(
    name := "Runner",
    libraryDependencies ++= Seq(
      deps.FS2,
      deps.catsEffect,
      deps.test.scalaTest,
      deps.test.catsEffectTesting
    ) ++ deps.dockerJava ++ deps.logging ++ deps.pureConfig
  )
  .dependsOn(util)

lazy val restApi = project
  .in(file("rest-api"))
  .settings(
    name := "REST API",
    libraryDependencies ++= Seq(
      deps.FS2,
      deps.catsEffect,
      deps.test.scalaTest,
      deps.test.catsEffectTesting
    ) ++ deps.http4sServer ++ deps.logging ++ deps.pureConfig
  )
  .dependsOn(runner, util)

val deps = new {

  lazy val V = new {
    val http4s = "0.23.11"
    val circe = "0.15.0-M1"
    val pureConfig = "0.17.1"
    val dockerJava = "3.2.13"
  }

  val dockerJava = Seq("docker-java", "docker-java-transport-zerodep").map(
    "com.github.docker-java" % _ % V.dockerJava
  )

  val FS2 = "co.fs2" %% "fs2-core" % "3.2.5"

  val catsEffect = "org.typelevel" %% "cats-effect" % "3.3.11"

  val pureConfig = Seq("com.github.pureconfig" %% "pureconfig-core" % V.pureConfig)

  val commonsIO = "commons-io" % "commons-io" % "2.11.0"

  val logging = Seq(
    "org.typelevel" %% "log4cats-slf4j" % "2.2.0",
    "org.slf4j" % "slf4j-simple" % "2.0.0-alpha6"
  )

  val http4sServer =
    Seq("http4s-dsl", "http4s-blaze-server", "http4s-circe").map(
      "org.http4s" %% _ % V.http4s
    ) :+ "io.circe" %% "circe-generic" % V.circe

  val test = new {
    val scalaTest = "org.scalatest" %% "scalatest" % "3.2.11" % "test"
    val catsEffectTesting = "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0"
  }
}
