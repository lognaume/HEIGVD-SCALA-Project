name := "SCALA_Play_Framework_Example"

version := "1.0"

lazy val `scala_play_framework_example` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(ehcache, ws, specs2 % Test, guice, evolutions)

libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.1"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "3.0.1"
libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.11"
unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")
