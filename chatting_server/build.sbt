import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.12",
    libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.12" % Test,
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.12",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.12" % Test

assemblyJarName in assembly := "hello_2.12-0.1.0-SNAPSHOT.jar"

)