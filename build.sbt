import Dependencies._

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version      := "0.0.1"
ThisBuild / organization := "cat-ai"
ThisBuild / description  := "Purely functional ZIO interface over Linux Epoll via JNA"

lazy val root =
  (project in file("."))
    .settings(
      name                :=  "zio-epoll",
      libraryDependencies ++= dependencies
    )

bintrayOrganization := Some("cat-ai")
bintrayRepository   := "zio"
publishTo           := Some("bintray" at "https://api.bintray.com/maven/cat-ai/zio/zio-epoll/;publish=1")
credentials         += Credentials(Path.userHome / ".sbt" / ".credentials")
publishMavenStyle   := true
publishArtifact     := true
licenses            += ("MIT", url("http://opensource.org/licenses/MIT"))
homepage            := Some(url("https://github.com/cat-ai/zio-epoll"))