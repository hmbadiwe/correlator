package com.example.service.correlation.persistence

import com.example.service.persistence.lucene.{LuceneAlarmPersistence, AlarmPersistence}
import akka.actor.ActorRef
import scalaxx.util._
import akka.pattern.ask
import scala.concurrent.Await
import org.slf4j.LoggerFactory
import akka.util.Timeout
import scala.concurrent.duration._
import scalaxx.util.SearchIndexedMessage

class CorrelationPersistenceService( persistenceActorRef : ActorRef ) extends AlarmPersistence {
  lazy val log = LoggerFactory.getLogger( getClass )
  implicit val timeout : Timeout = 90 seconds

  override def save( alarm: CommonMessage ) { persistenceActorRef ! alarm  }

  override def update( alarm: CommonMessage ) {
    persistenceActorRef ! UpdateMessage( alarm )
  }

  override def findByParameter( params : Map[ String, List[ String ] ]): List[ CommonMessage ] = {
    val actorRef = persistenceActorRef

    /*
      //TODO Time sorted query. Will reconsider
      val future = ( actorRef ? TimeLimitedSearchIndexedMessage( params, Some( 10 )) ).mapTo[ CommonMessageResults ]
    */
    val future = ( actorRef ? SearchIndexedMessage( params ) ).mapTo[ CommonMessageList ]
    try {
      val result = Await.result( future, timeout.duration )
      result.messageList
    }
    catch {
      case t: Throwable => {
        log.error( "Error waiting for future to return. Returning Nil", t)
        Nil
      }
    }
  }

  override def query( queryString : String ) : List[ CommonMessage ] = {
    val actorRef = persistenceActorRef
    val future = ( actorRef ? IndexQuery( queryString) ).mapTo[ CommonMessageList ]
    try {
      val result = Await.result( future, timeout.duration )
      result.messageList
    }
    catch {
      case t: Throwable => {
        log.error( "Error waiting for future to return. Returning Nil", t)
        Nil
      }
    }
  }
}
