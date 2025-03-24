ThisBuild / scalaVersion := "2.13.16"
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
ThisBuild / sonatypeCredentialHost := "oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://oss.sonatype.org/service/local"
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / homepage := Some(url("https://github.com/lectra-tech/kapoeira"))
ThisBuild / developers := List(
  Developer("jvauchel", "Johanna Vauchel", "j.vauchel@lectra.com", url("https://github.com/jvauchel")),
  Developer("sebastienvidal", "Sébastien Vidal","s.vidal@lectra.com",url("https://github.com/sebastienvidal")),
  Developer("mrebiai", "Mehdi Rebiai", "m.rebiai@lectra.com", url("https://github.com/mrebiai")),
  Developer("scarisey", "Sylvain Carisey", "s.carisey@lectra.com", url("https://github.com/scarisey")),
)

resolvers += "confluent" at "https://packages.confluent.io/maven/"
val zioVersion = "2.1.16"

lazy val root = (project in file("."))
  .settings(
    name := "kapoeira",
    // assembly
    assembly / assemblyJarName := "kapoeira.jar",
    assembly / test  := {},
    assembly / mainClass := Some("io.cucumber.core.cli.Main"),
    // confluent
    libraryDependencies += "io.confluent" % "kafka-avro-serializer" % "7.2.14" exclude("javax.ws.rs", "javax.ws.rs-api"),
    libraryDependencies += "io.confluent" % "kafka-json-schema-serializer" % "7.2.14" exclude("javax.ws.rs", "javax.ws.rs-api"),
    // more libs to include
    // https://github.com/confluentinc/schema-registry/blob/master/pom.xml
    libraryDependencies ++= Seq(
      "org.apache.kafka" %% "kafka" % "3.2.3",
      "io.cucumber" %% "cucumber-scala" % "8.26.2",
      "org.scalatest" %% "scalatest" % "3.2.19",
      "com.typesafe" % "config" % "1.4.3",
      "io.gatling" % "gatling-jsonpath" % "3.13.5",
      "com.lihaoyi" %% "os-lib" % "0.11.4",
      "ch.qos.logback" % "logback-classic" % "1.5.18" % Runtime,
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-logging-slf4j2" % "2.5.0",
    ),
    // only tests
    libraryDependencies ++= Seq(
      "io.cucumber" % "cucumber-junit" % "7.21.1",
      "org.scalamock" %% "scalamock" % "7.3.0",
      "org.scalacheck" %% "scalacheck" % "1.18.1",
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

