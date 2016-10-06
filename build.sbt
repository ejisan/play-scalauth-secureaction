name := """play-scalauth-secureaction"""

organization := "com.ejisan"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

crossScalaVersions := Seq(scalaVersion.value)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.5.8" % Provided,
  "com.typesafe.play" %% "play-specs2" % "2.5.8" % Test
)

publishTo := Some(Resolver.file("ejisan", file(Path.userHome.absolutePath+"/Development/repo.ejisan"))(Patterns(true, Resolver.mavenStyleBasePattern)))
