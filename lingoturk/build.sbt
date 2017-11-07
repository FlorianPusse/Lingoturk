name := """Lingoturk"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava, PlayEbean, LauncherJarPlugin)

scalaVersion := "2.12.3"

libraryDependencies += guice
libraryDependencies += javaJdbc
libraryDependencies += ehcache

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.1.1",
  "org.apache.commons" % "commons-lang3" % "3.6",
  "com.amazonaws" % "aws-java-sdk" % "1.9.13",
  "commons-io" % "commons-io" % "2.6",
  "net.lingala.zip4j" % "zip4j" % "1.3.1",
  "org.mindrot" % "jbcrypt" % "0.4",
  "com.opencsv" % "opencsv" % "4.0",
  "be.objectify" %% "deadbolt-java" % "2.6.1"
)

// Testing libraries for dealing with CompletionStage...
libraryDependencies += "org.assertj" % "assertj-core" % "3.6.2" % Test
libraryDependencies += "org.awaitility" % "awaitility" % "2.0.0" % Test

// Make verbose tests
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v"))


fork in run := true