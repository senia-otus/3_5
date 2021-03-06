import sbt._

object ProjectConfig {

  object versions {
    val akka        = "2.6.9"
    val `akka-http` = "10.2.0"

    val `akka-http-play-json` = "1.34.0"
    val `play-json`           = "2.9.0"

    val scalactic         = "3.2.0"
    val scalatest         = "3.2.0"
    val scalacheck        = "1.14.3"
    val `scalacheck-1-14` = "3.2.0.0"
    val scalamock         = "5.0.0"

    val `scalacheck-shapeless_1.14` = "1.2.3"

    val doobie = "0.9.0"

    val slick = "3.3.2"

    val postgresql = "42.2.15"

    val `flyway-core` = "6.5.5"

    val logback = "1.2.3"

    val slf4j = "1.7.30"

    val testcontainers = "0.38.4"

    val zio = "1.0.1"

    val `zio-config` = "1.0.0-RC27"

    val `zio-logging` = "0.5.2"
  }

  val testDependencies = Seq(
    "org.scalactic"              %% "scalactic"                 % versions.scalactic                   % Test,
    "org.scalatest"              %% "scalatest"                 % versions.scalatest                   % Test,
    "org.scalacheck"             %% "scalacheck"                % versions.scalacheck                  % Test,
    "org.scalatestplus"          %% "scalacheck-1-14"           % versions.`scalacheck-1-14`           % Test,
    "org.scalamock"              %% "scalamock"                 % versions.scalamock                   % Test,
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % versions.`scalacheck-shapeless_1.14` % Test
  )

  val akkaDependencies = Seq(
    "com.typesafe.akka" %% "akka-stream"              % versions.akka,
    "com.typesafe.akka" %% "akka-http"                % versions.`akka-http`,
    "com.typesafe.akka" %% "akka-stream-testkit"      % versions.akka,
    "com.typesafe.akka" %% "akka-http-testkit"        % versions.`akka-http`,
    "com.typesafe.akka" %% "akka-stream"              % versions.akka,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % versions.akka % Test
  )

  val playJsonDependencies = Seq(
    "de.heikoseeberger" %% "akka-http-play-json" % versions.`akka-http-play-json`,
    "com.typesafe.play" %% "play-json"           % versions.`play-json`
  )

  val doobieDependencies = Seq(
    "org.tpolecat" %% "doobie-core"      % versions.doobie,
    "org.tpolecat" %% "doobie-hikari"    % versions.doobie,
    "org.tpolecat" %% "doobie-quill"     % versions.doobie,
    "org.tpolecat" %% "doobie-postgres"  % versions.doobie,
    "org.tpolecat" %% "doobie-scalatest" % versions.doobie % Test
  )

  val slickDependencies = Seq(
    "com.typesafe.slick" %% "slick"          % versions.slick,
    "com.typesafe.slick" %% "slick-hikaricp" % versions.slick
  )

  val dbDependencies = Seq(
    "org.postgresql" % "postgresql"  % versions.postgresql,
    "org.flywaydb"   % "flyway-core" % versions.`flyway-core`,
//    "com.dimafeng"  %% "testcontainers-scala-scalatest"  % versions.testcontainers % Test,
//    "com.dimafeng" %% "testcontainers-scala-postgresql" % versions.testcontainers % Test,
    //https://github.com/testcontainers/testcontainers-java/issues/3166
    "org.testcontainers" % "testcontainers" % "1.15.0-rc2" % Test,
    "org.testcontainers" % "postgresql"     % "1.15.0-rc2" % Test
  )

  val logDependencies = Seq(
    "ch.qos.logback" % "logback-classic" % versions.logback,
    "org.slf4j"      % "slf4j-api"       % versions.slf4j
  )

  val zioDependencies = Seq(
    "dev.zio"    %% "zio"                 % versions.zio,
    "dev.zio"    %% "zio-macros"          % versions.zio,
    "dev.zio"    %% "zio-streams"         % versions.zio,
    "dev.zio"    %% "zio-config"          % versions.`zio-config`,
    "dev.zio"    %% "zio-config-magnolia" % versions.`zio-config`,
    "dev.zio"    %% "zio-config-typesafe" % versions.`zio-config`,
    "dev.zio"    %% "zio-test"            % versions.zio % Test,
    "dev.zio"    %% "zio-test-sbt"        % versions.zio % Test,
    "dev.zio"    %% "zio-test-magnolia"   % versions.zio % Test,
    "dev.zio"    %% "zio-logging"         % versions.`zio-logging`,
    "dev.zio"    %% "zio-logging-slf4j"   % versions.`zio-logging`,
    "info.senia" %% "zio-test-akka-http"  % "0.2.0"
  )

  val projectDependencies =
    testDependencies ++
      akkaDependencies ++
      playJsonDependencies ++
      doobieDependencies ++
      slickDependencies ++
      dbDependencies ++
      logDependencies ++
      zioDependencies
}
