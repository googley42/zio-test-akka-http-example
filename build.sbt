name := "zio-test-akka-example"

version := "0.1"

scalaVersion := "2.12.11"

addCompilerPlugin(("org.scalamacros" % "paradise" % "2.1.1") cross CrossVersion.full)

val CirceVersion = "0.12.1"
val ZioVersion = "1.0.6"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-macros" % ZioVersion,
  "dev.zio" %% "zio-streams" % ZioVersion,
  "dev.zio" %% "zio-test" % ZioVersion % "test",
  "dev.zio" %% "zio-test-sbt" % ZioVersion % "test",
  "dev.zio" %% "zio-logging-slf4j" % "0.5.8",
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
  "info.senia" %% "zio-test-akka-http" % "1.0.1"
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
