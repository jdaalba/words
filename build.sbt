ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "words"
  )

libraryDependencies += "org.neo4j.driver" % "neo4j-java-driver" % "5.8.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.3.0-SNAP4" % Test
libraryDependencies +=  "org.seleniumhq.selenium" %  "selenium-java" % "3.4.0"