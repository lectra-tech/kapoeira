ThisBuild / scalaVersion := "2.13.18"
ThisBuild / organization := "com.lectra"
ThisBuild / organizationName := "lectra"
ThisBuild / licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(true)
ThisBuild / publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/lectra-tech/kapoeira"),
    "scm:git:git@github.com/lectra-tech/kapoeira.git"
  )
)
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / homepage := Some(url("https://github.com/lectra-tech/kapoeira"))
ThisBuild / developers := List(
  Developer("jvauchel", "Johanna Vauchel", "j.vauchel@lectra.com", url("https://github.com/jvauchel")),
  Developer("sebastienvidal", "Sébastien Vidal","s.vidal@lectra.com",url("https://github.com/sebastienvidal")),
  Developer("mrebiai", "Mehdi Rebiai", "m.rebiai@lectra.com", url("https://github.com/mrebiai")),
  Developer("scarisey", "Sylvain Carisey", "s.carisey@lectra.com", url("https://github.com/scarisey")),
)

resolvers += "confluent" at "https://packages.confluent.io/maven/"
val zioVersion = "2.1.24"

lazy val root = (project in file("."))
  .settings(
    name := "kapoeira",
    // assembly
    assembly / assemblyJarName := "kapoeira.jar",
    assembly / test  := {},
    assembly / mainClass := Some("io.cucumber.core.cli.Main"),
    // confluent
    libraryDependencies += "io.confluent" % "kafka-avro-serializer" % "7.9.5" exclude("javax.ws.rs", "javax.ws.rs-api"),
    libraryDependencies += "io.confluent" % "kafka-json-schema-serializer" % "7.9.5" exclude("javax.ws.rs", "javax.ws.rs-api"),
    // more libs to include
    // https://github.com/confluentinc/schema-registry/blob/master/pom.xml
    libraryDependencies ++= Seq(
      "org.apache.kafka" %% "kafka" % "3.9.1",
      "io.cucumber" %% "cucumber-scala" % "8.38.0",
      "org.scalatest" %% "scalatest" % "3.2.19",
      "com.typesafe" % "config" % "1.4.5",
      "io.gatling" % "gatling-jsonpath" % "3.14.9",
      "com.lihaoyi" %% "os-lib" % "0.11.6",
      "ch.qos.logback" % "logback-classic" % "1.5.24" % Runtime,
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-logging-slf4j2" % "2.5.2",
    ),
    // only tests
    libraryDependencies ++= Seq(
      "io.cucumber" % "cucumber-junit" % "7.33.0",
      "org.scalamock" %% "scalamock" % "7.5.2",
      "org.scalacheck" %% "scalacheck" % "1.19.0",
      "dev.zio" %% "zio-test" % zioVersion,
      "dev.zio" %% "zio-test-sbt" % zioVersion
    ).map(_ % Test),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

// assembly config
ThisBuild / assemblyMergeStrategy := {
  case PathList(ps@_*) if ps.last == "module-info.class" => MergeStrategy.discard
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case "META-INF/FastDoubleParser-NOTICE" => MergeStrategy.first
  case "kafka/kafka-version.properties" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case "logback.xml" => MergeStrategy.first
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

