name := "blackbox-backend"
ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "3.3.1"
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
  "-source:3.2"
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

lazy val utilStorage = project
  .in(file("storage-oc"))
  .settings(
    name := "Storage Owncloud",
    libraryDependencies ++= Seq(
      deps.catsEffect,
      deps.test.scalaTest,
      deps.test.catsEffectTesting,
      deps.test.scalaMockito,
      deps.FS2,
      deps.fs2IO,
      deps.betterFiles,
      deps.scalaUri
    ) ++ deps.pureConfig ++ deps.logging ++ deps.sardine
      ++ deps.circeAll
  )

lazy val utilFlow = project
  .in(file("flow-gh"))
  .settings(
    // Should it become Flow Github? ;-)
    name := "Flow Github",
    libraryDependencies ++= Seq(
      deps.catsEffect,
      deps.test.scalaTest,
      deps.test.catsEffectTesting,
      deps.test.scalaMockito,
      deps.FS2,
      deps.fs2IO,
      deps.betterFiles,
      deps.scalaUri,
      deps.github4s
    ) ++ deps.pureConfig ++ deps.logging ++ deps.sardine
      ++ deps.circeAll
  )

lazy val runner = project
  .in(file("runner"))
  .settings(
    name := "Runner",
    libraryDependencies ++= Seq(
      deps.FS2,
      deps.fs2IO,
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
    val http4s = "0.23.23"
    val http4sBlaze = "0.23.15"
    val circe = "0.15.0-M1"
    val pureConfig = "0.17.4"
    val dockerJava = "3.2.14"
    val fs2 = "3.3.0"
  }

  val dockerJava = Seq("docker-java", "docker-java-transport-zerodep").map(
    "com.github.docker-java" % _ % V.dockerJava
  )

  val FS2 = "co.fs2" %% "fs2-core" % "3.7.0"

  val fs2IO = "co.fs2" %% "fs2-io" % "3.7.0"

  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.1"

  val catsEffectFiles = "io.github.akiomik" %% "cats-nio-file" % "1.7.0"

  val pureConfig = Seq(
    "com.github.pureconfig" %% "pureconfig-core" % V.pureConfig,
    "com.github.pureconfig" %% "pureconfig-cats" % "0.17.4"
  )

  val commonsIO = "commons-io" % "commons-io" % "2.11.0"

  val logging = Seq(
    "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
    "org.slf4j" % "slf4j-simple" % "2.0.9"
  )

  val circeGeneric = "io.circe" %% "circe-generic" % V.circe

  val circeAll = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-parser"
  ).map(_ % V.circe) ++ Seq(circeGeneric)

  val http4sServer =
    Seq(
      "org.http4s" %% "http4s-dsl" % V.http4s,
      "org.http4s" %% "http4s-blaze-server" % V.http4sBlaze,
      "org.http4s" %% "http4s-circe" % V.http4s
    ) :+ circeGeneric

  val sardine = Seq(
    "com.github.lookfirst" % "sardine" % "5.9",
    "javax.xml.bind" % "jaxb-api" % "2.4.0-b180830.0359",
    "javax.activation" % "activation" % "1.1.1",
    "org.glassfish.jaxb" % "jaxb-runtime" % "2.4.0-b180830.0438"
  )

  val scalaUri = "io.lemonlabs" %% "scala-uri" % "4.0.3"

  val github4s = "com.47deg" %% "github4s" % "0.32.0"

  val test = new {
    val scalaTest = "org.scalatest" %% "scalatest" % "3.2.17" % "test"
    val catsEffectTesting = "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % "test"
    val scalaMockito = "org.scalatestplus" %% "mockito-4-11" % "3.2.17.0" % "test"
  }

  val betterFiles = "com.github.pathikrit" %% "better-files" % "3.9.2"
}
