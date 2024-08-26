ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "com.lectra.kafka"
ThisBuild / organizationName := "lectra"
ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(true)
ThisBuild / publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

resolvers += "confluent" at "https://packages.confluent.io/maven/"

val zioVersion = "2.1.8"

lazy val root = (project in file("."))
  .enablePlugins(GitVersioning)
  .settings(
    name := "kapoeira",
    // assembly
    assembly / assemblyJarName := "kapoeira.jar",
    assembly / mainClass := Some("io.cucumber.core.cli.Main"),
    // confluent
    libraryDependencies += "io.confluent" % "kafka-avro-serializer" % "7.2.11" exclude("javax.ws.rs", "javax.ws.rs-api"),
    libraryDependencies += "io.confluent" % "kafka-json-schema-serializer" % "7.2.11" exclude("javax.ws.rs", "javax.ws.rs-api"),
    // more libs to include
    // https://github.com/confluentinc/schema-registry/blob/master/pom.xml
    libraryDependencies ++= Seq(
      "org.apache.kafka" %% "kafka" % "3.2.3",
      "io.cucumber" %% "cucumber-scala" % "8.23.1",
      "org.scalatest" %% "scalatest" % "3.2.19",
      "com.typesafe" % "config" % "1.4.3",
      "io.gatling" % "gatling-jsonpath" % "3.11.5",
      "com.lihaoyi" %% "os-lib" % "0.10.4",
      "ch.qos.logback" % "logback-classic" % "1.5.7" % Runtime,
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-logging-slf4j2" % "2.3.1",
    ),
    // only tests
    libraryDependencies ++= Seq(
      "io.cucumber" % "cucumber-junit" % "7.18.1",
      "org.scalamock" %% "scalamock" % "6.0.0",
      "org.scalacheck" %% "scalacheck" % "1.18.0",
      "dev.zio" %% "zio-test" % zioVersion,
      "dev.zio" %% "zio-test-sbt" % zioVersion
    ).map(_ % Test),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

// assembly config
ThisBuild / assemblyMergeStrategy := {
  case PathList(ps@_*) if ps.last == "module-info.class" => MergeStrategy.discard
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case "kafka/kafka-version.properties" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case "logback.xml" => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

// git config
git.useGitDescribe := true
