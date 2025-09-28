ThisBuild / scalaVersion := "2.13.15"
ThisBuild / organization := "org.example"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val CatsEffectVersion  = "3.5.4"
lazy val Fs2KafkaVersion    = "3.6.0"
lazy val LogbackVersion     = "1.5.6"
lazy val SlickVersion       = "3.5.1"
lazy val PostgresDriverVer  = "42.7.3"
lazy val CirceVersion       = "0.14.9"

lazy val commonLibs = Seq(
  "org.typelevel"  %% "cats-effect"     % CatsEffectVersion,
  "com.github.fd4s"%% "fs2-kafka"       % Fs2KafkaVersion,
  "ch.qos.logback" %  "logback-classic" % LogbackVersion
)

lazy val root = (project in file("."))
  .aggregate(generator, radiometer)
  .settings(
    name := "radiation-project",
    publish / skip := true
  )

lazy val generator = (project in file("modules/generator"))
  .settings(
    name := "generator",
    libraryDependencies ++= commonLibs ++ Seq(
      // генератор сериализует события в JSON
      "io.circe" %% "circe-core"    % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser"  % CirceVersion
    ),
    Compile / run / mainClass := Some("RadiationSourcesApp")

  )

lazy val radiometer = (project in file("modules/radiometer"))
  .settings(
    name := "radiometer",
    libraryDependencies ++=
      commonLibs ++ Seq(
        // Slick + HikariCP + Postgres
        "com.typesafe.slick" %% "slick"          % SlickVersion,
        "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
        "org.postgresql"      % "postgresql"     % PostgresDriverVer,
        // SLF4J + Logback
        "ch.qos.logback" % "logback-classic" % "1.5.6" % Runtime,
        // Circe для EventParser
        "io.circe" %% "circe-core"    % CirceVersion,
        "io.circe" %% "circe-generic" % CirceVersion,
        "io.circe" %% "circe-parser"  % CirceVersion
      ),
    Compile / run / mainClass := Some("radiometer.app.RadiometerApp")
  )
