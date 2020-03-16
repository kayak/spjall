lazy val akkaHttpVersion = "10.1.8"
lazy val akkaVersion    = "2.5.21"
lazy val circeVersion = "0.11.1"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.r9",
      scalaVersion    := "2.11.12"
    )),
    name := "spjall",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "requests" % "0.1.7",
      "com.typesafe" % "config" % "1.3.2",

      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,

      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      //"com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test
    )
  )
