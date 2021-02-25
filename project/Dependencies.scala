import sbt._

object Dependencies {

  val zioVersion = "1.0.3"
  val jnaVersion = "5.7.0"

  val dependencies = Seq(
    "dev.zio"                 %% "zio"                     % zioVersion,
    "dev.zio"                 %% "zio-streams"             % zioVersion,
    "dev.zio"                 %% "zio-nio"                 % "1.0.0-RC10",
    "cat-ai"                  %% "zio-linux"               % "0.0.3",
    "org.scala-lang.modules"  %% "scala-collection-compat" % "2.2.0",
    "org.apache.logging.log4j" % "log4j-api"               % "2.13.1"   % Test,
    "org.apache.logging.log4j" % "log4j-core"              % "2.13.1"   % Test,
    "org.apache.logging.log4j" % "log4j-slf4j-impl"        % "2.13.1"   % Test,
    "dev.zio"                 %% "zio-test"                % zioVersion % Test,
    "dev.zio"                 %% "zio-test-sbt"            % zioVersion % Test,
    "net.java.dev.jna"         % "jna"                     % jnaVersion,
    "org.jsoup"                % "jsoup"                   % "1.13.1",
    "org.bouncycastle"         % "bcpkix-jdk15on"          % "1.61"     % Optional,
    "org.bouncycastle"         % "bcmail-jdk15on"          % "1.61"     % Optional
  )
}