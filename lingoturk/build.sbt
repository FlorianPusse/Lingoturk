name := "lingoturk"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "org.apache.commons" % "commons-lang3" % "3.3.2",
  "com.amazonaws" % "aws-java-sdk" % "1.9.13",
  "org.apache.httpcomponents" % "httpclient" % "4.3.6",
  "org.apache.httpcomponents" % "httpcore" % "4.3.3",
  "commons-io" % "commons-io" % "2.4",
  "net.lingala.zip4j" % "zip4j" % "1.3.1"
)