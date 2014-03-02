import sbt._
import sbt.Keys._
object CorrelationBuild extends Build{
	
	
	lazy val buildSettings = Defaults.defaultSettings ++ Seq(
		organization := "supersymmetry",
		version := "1.0",
		scalaVersion := "2.10.1",
		shellPrompt := ShellPrompt.buildShellPrompt
	)

	lazy val defaultSettings = buildSettings ++ Seq(
		resolvers += "Typesafe Release Repo" at "http://repo.typesafe.com/typesafe/releases/",
		scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature", "-language:postfixOps"),
    	javacOptions  ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
	)

	lazy val CorrelationProject = Project(
		id = "correlation",
		base = file( "."),
		settings = defaultSettings
	) aggregate( Persistence, Correlator, Rest )

	lazy val Persistence = Project(
		id = "persistence",
		base = file( "persistence"),
		settings = defaultSettings ++ Seq( libraryDependencies ++= Dependencies.persistenceDeps )
	)
	lazy val Correlator = Project(
		id = "correlator",
		base = file( "correlator"),
		settings = defaultSettings ++ Seq( libraryDependencies ++= Dependencies.correlatorDeps )
	)
	lazy val Rest = Project(
		id = "rest",
		base = file( "rest"),
		settings = defaultSettings ++ Seq( libraryDependencies ++= Dependencies.restDeps )
	)
}

object ShellPrompt {
  object devnull extends ProcessLogger {
    def info (s: => String) {}
    def error (s: => String) { }
    def buffer[T] (f: => T): T = f
  }
  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
      getOrElse "-" stripPrefix "## " split( "\\.{3}") head
    )

  val buildShellPrompt = {
    (state: State) => {
      val currProject = Project.extract (state).currentProject.id
      "%s::%s> ".format ( currBranch, currProject   )
    }
  }
}

object Dependencies {
	object Versions {
		val akkaVersion = "2.2.3"
		val scalaTest = "2.0"
		val unfiltered = "0.6.8"
		val lucene = "4.3.0"
		val commonsLang3 = "3.2.1"
		val slf4j = "1.7.6"
		val logback = "1.1.1"
		val scala = "2.10.1"
    val jodaTime = "2.3"
	}

	val commonsLang = "org.apache.commons" % "commons-lang3" % Versions.commonsLang3
	val unfiltered = "net.databinder" %% "unfiltered" % Versions.unfiltered
	val slf4j = "org.slf4j" % "slf4j-api" % Versions.slf4j
	val logbackClassic = "ch.qos.logback" % "logback-classic" % Versions.logback
	val logbackCore = "ch.qos.logback" % "logback-core" % Versions.logback
	val akka = "com.typesafe.akka" %% "akka-actor" % Versions.akkaVersion
	val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalaTest % "test"
	val luceneCore = "org.apache.lucene" % "lucene-core" % Versions.lucene
	val luceneAnalyzers = "org.apache.lucene" % "lucene-analyzers-common" % Versions.lucene
	val luceneQueryParser = "org.apache.lucene" % "lucene-queryparser" % Versions.lucene
  val jodaTime  = "joda-time"     % "joda-time"               % Versions.jodaTime

	val commonDeps = Seq( logbackCore, logbackClassic, slf4j, scalaTest, akka, jodaTime)
	val restDeps = Seq( unfiltered ) ++ commonDeps
	val persistenceDeps = Seq( luceneCore, luceneAnalyzers, luceneQueryParser ) ++ commonDeps
	val correlatorDeps = commonDeps
	


}
