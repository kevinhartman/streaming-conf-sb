name := "streaming-conf-sb"

scalaVersion := "2.11.7"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-unchecked",
  "-deprecation",
  "-feature"
)

val sparkVersion = "2.2.0"

// Dependencies provided by the Spark distro
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion
)

// Bundled dependencies
libraryDependencies ++= Seq(
  "com.microsoft.azure" % "azure-servicebus" % "1.1.0",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0",
  "net.liftweb" %% "lift-json" % "3.0.1"
)