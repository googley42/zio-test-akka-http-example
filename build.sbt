name := "zio-test-akka-example"

version := "0.1"

scalaVersion := "2.12.11"

addCompilerPlugin(("org.scalamacros" % "paradise" % "2.1.1") cross CrossVersion.full)

val CirceVersion = "0.12.1"
val ZioVersion = "1.0.0-RC18-2"
val SttpVersion = "2.0.9"

/*
    private val Version = "0.12.1"
    val circeCore = "io.circe" %% "circe-core" % Version
    val circeGeneric = "io.circe" %% "circe-generic" % Version
    val circeParser = "io.circe" %% "circe-parser" % Version
    val circeGenericExtras = "io.circe" %% "circe-generic-extras" % Version
 */

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-streams" % ZioVersion,
  "dev.zio" %% "zio-test" % ZioVersion % "test",
  "dev.zio" %% "zio-test-sbt" % ZioVersion % "test",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.3",
  "dev.zio" %% "zio-logging-slf4j" % "0.2.8",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-http" % "10.1.9",
  "com.typesafe.akka" %% "akka-stream" % "2.5.23",
  "de.heikoseeberger" %% "akka-http-circe" % "1.27.0",
  "io.circe" %% "circe-core" % CirceVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-parser" % CirceVersion,
  "io.circe" %% "circe-generic-extras" % CirceVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.3" % Test,
  "info.senia" %% "zio-test-akka-http" % "0.1.0",
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
