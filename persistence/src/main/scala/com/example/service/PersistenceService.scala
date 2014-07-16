package com.example.service

import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import com.typesafe.config.ConfigFactory
import scala.util.Try
import org.apache.lucene.store.{Directory, RAMDirectory}
import com.example.actor.PersistenceActor


/**
 * Created with IntelliJ IDEA.
 * User: hmbadiwe
 * Date: 3/1/14
 * Time: 8:46 PM
 * To change this template use File | Settings | File Templates.
 */

trait PersistenceService extends SetupDirectory {  this: Actor with ActorLogging=>

  val routerName = "restPersister"
  val luceneDirKey = "lucene.basedir"
  val actorPrefix = "mod_actor_"

  def configLoad() = {
    val config = ConfigFactory.load().getConfig("AkkaRestPersistence")
    config
  }

  def configPort(): Int = {
    val port = ConfigFactory.load().getInt("httpPort")
    log.debug(s"Config port found: ${port}")
    port
  }

  def configNumberRoutees(): Int = {
    val numberRoutees = ConfigFactory.load().getInt("actorRoutees")
    log.debug(s"Number of actor routees found: ${numberRoutees}")
    numberRoutees
  }

  def configLuceneDirectory(): Option[String] = {
    Option( System.getProperty( luceneDirKey ) ) match {
      case Some( luceneDir ) => {
        log.debug( s"Lucene base directory specified in lucene.basedir system environment variable: ${luceneDir}")
        Some(luceneDir)
      }
      case _ => {
        Try{
          val config =  ConfigFactory.load()
          val baseDirectory = config.getString("luceneDirectory")
          log.debug(s"Lucene directory specified in config file: ${baseDirectory}")
          baseDirectory
        }.toOption
      }
    }
  }

  def createRestfulPersistenceActors : List[ ActorRef ] = {
    val numberOfActors = configNumberRoutees()
    val baseDirectoryOption = configLuceneDirectory()
    require(numberOfActors > 0, "The number of actors specified must be greater than zero")
    if( baseDirectoryOption == None) log.debug( "No lucene directories specified. Using ram directories")

    val actorSequence = (0 until numberOfActors) map { indexNum =>
      val indexDirectory =   baseDirectoryOption.map { baseDirectory =>
        val filePath = baseDirectory + "/" + indexNum
        createDirectory(  filePath, false )
      } getOrElse( setupDirectory(new RAMDirectory()))

      val actorRef = context.actorOf(
        Props(
          new PersistenceActor { val directory: Directory = indexDirectory }
        ),
        actorPrefix + indexNum
      )
      actorRef
    }
    actorSequence.toList
  }


}