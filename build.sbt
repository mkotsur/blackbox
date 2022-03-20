val scala3Version = "3.1.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "Black box",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
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

val deps = new {
  val dockerJava = "com.github.docker-java" % "docker-java" % "3.2.13"
  val dockerJavaTransport = "com.github.docker-java" % "docker-java-transport-zerodep" % "3.2.13"
  val FS2 = "co.fs2" %% "fs2-core" % "3.2.5"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.3.7"
  val log4cats = "org.typelevel" %% "log4cats-slf4j" % "2.2.0"
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % "2.0.0-alpha6"

  val test = new {
    val scalaTest = "org.scalatest" %% "scalatest" % "3.2.11" % "test"
    val catsEffectTesting = "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0"
  }
}
