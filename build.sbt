import scala.io.Source

val projectVersion = {
  val versionFile = Source.fromFile("./version.txt")
  val version = versionFile.getLines.mkString
  versionFile.close()
  version
}

ThisBuild / version := projectVersion
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "com.lectra.kafka"
ThisBuild / organizationName := "lectra"
ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(true)
ThisBuild / publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

resolvers += "confluent" at "https://packages.confluent.io/maven/"

val zioVersion = "2.0.20"

lazy val root = (project in file("."))
  .settings(
    name := "kapoeira",
    // confluent
    libraryDependencies += "io.confluent" % "kafka-avro-serializer" % "7.2.9" exclude("javax.ws.rs", "javax.ws.rs-api"),
    libraryDependencies += "io.confluent" % "kafka-json-schema-serializer" % "7.2.9" exclude("javax.ws.rs", "javax.ws.rs-api"),
    // more libs to include
    // https://github.com/confluentinc/schema-registry/blob/master/pom.xml
    libraryDependencies += "org.apache.kafka" %% "kafka" % "3.2.3",
    libraryDependencies += "io.cucumber" %% "cucumber-scala" % "6.10.4",
    libraryDependencies += "io.cucumber" % "cucumber-junit" % "6.10.4",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.17",
    libraryDependencies += "com.typesafe" % "config" % "1.4.3",
    libraryDependencies += "io.gatling" % "gatling-jsonpath" % "3.10.3",
    libraryDependencies += "com.lihaoyi" %% "requests" % "0.7.1",
    libraryDependencies += "com.lihaoyi" %% "ammonite-ops" % "2.4.1",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.14" % Runtime,
    libraryDependencies += "dev.zio" %% "zio" % zioVersion,
    libraryDependencies += "dev.zio" %% "zio-streams" % zioVersion,
    libraryDependencies += "dev.zio" %% "zio-logging-slf4j2" % "2.1.16",
    // only tests
    libraryDependencies += "org.scalamock" %% "scalamock" % "5.2.0" % Test,
    libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test" % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

// assembly config
assembly / assemblyJarName := "kapoeira.jar"
assembly / assemblyMergeStrategy := {
  case "module-info.class" => MergeStrategy.discard
  case x if x.endsWith("/module-info.class") => MergeStrategy.discard
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case "kafka/kafka-version.properties" => MergeStrategy.first
  case "application.conf" =>
    new sbtassembly.MergeStrategy {
      val name = "reverseConcat"

      def apply(
                 tempDir: File,
                 path: String,
                 files: Seq[File]
               ): Either[String, Seq[(File, String)]] =
        MergeStrategy.concat(tempDir, path, files.reverse)
    }
  case "logback.xml" => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)

}
assembly / mainClass := Some("io.cucumber.core.cli.Main")
