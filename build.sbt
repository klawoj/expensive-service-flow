
name := "senti1"

version := "0.1"

val scalaV = "2.12.2"
val akkaV = "2.5.22"
val scalazV = "7.2.14"
val scalaCheckV = "1.13.5"
val scalaTestV = "3.0.0"
val mockitoV = "1.10.19"
val logBackV = "1.2.3"

lazy val senti1 = project
  .in(file("."))
  .settings(
    scalaVersion := "2.12.2",
    name := "senti1-queues",
    organization := "pl.klawoj",
    libraryDependencies ++= {
      Seq(

        "com.typesafe.akka" %% "akka-actor" % akkaV,
        "com.typesafe.akka" %% "akka-stream" % akkaV,


        "com.typesafe.akka" %% "akka-testkit" % akkaV % Test,
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaV % Test,
        "org.scalacheck" %% "scalacheck" % scalaCheckV % Test,
        "org.scalatest" %% "scalatest" % scalaTestV % Test,
        "org.mockito" % "mockito-core" % mockitoV % Test,


        "org.scala-lang" % "scala-reflect" % scalaV,
        "org.scalaz" %% "scalaz-core" % scalazV,


        "com.typesafe.akka" %% "akka-slf4j" % akkaV,
        "ch.qos.logback" % "logback-classic" % logBackV
      )
    })