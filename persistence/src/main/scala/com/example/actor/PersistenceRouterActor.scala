package com.example.actor

import akka.actor.{ ActorLogging, ActorRef, Actor }
import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext, future}
import scala.concurrent.Future._
import akka.pattern.ask
import akka.pattern.pipe
import scala.util.Random
import akka.util.Timeout

import com.cisco.phalanx.persistence.service.PersistenceService
/**
 * Created with IntelliJ IDEA.
 * User: hmbadiwe
 * Date: 3/1/14
 * Time: 8:36 PM
 * To change this template use File | Settings | File Templates.
 */
class PersistenceRouterActor(  val childActorList : List[ ActorRef ] ) extends Actor with PersistenceService with ActorLogging{
  implicit val timeout : Timeout = 60 seconds
  implicit val executionContext : ExecutionContext = context.dispatcher

  override def preStart() {
    super.preStart()
    log.debug( s"Starting persistence router actor with path : ${self.path}")
  }

  def receive = {
    case sourceMessage : SourceMessage => {
      import JsonParserUtils._
      val preview = sourceMessage.string.slice( 0, 100)
      log.debug( s"Received SourceMessage with string: ${preview}")
      midFromJson(sourceMessage.string).foreach{ midParsedSource =>
        log.debug(s"Sending parsed mid back to self ${midParsedSource.toString.slice( 0, 100)}")
        self forward midParsedSource
      }
    }
    case c: CommonMessageList => {
      log.debug( s"Received CommonMessageList of size: ${ c.messageList.length }")
      for{
        msg <- c.messageList
        messageId <- msg.map.get("messageId")
        agentId <- msg.map.get("agentId")
      } yield {
        val actorIndex = MidParsedSourceMessage( agentId, messageId, null).hashCode % childActorList.size
        log.debug( s"Sending to actor with actor index: ${ actorIndex }")
        val actorRef = childActorList( actorIndex )
        actorRef forward msg
      }
    }
    case msg: CommonMessage => {
      log.debug( s"Received CommonMessage")
      for{
        messageId <- msg.map.get( "messageId" )
        agentId <- msg.map.get( "agentId" )
      } yield {
        val actorIndex = MidParsedSourceMessage( agentId, messageId, "").hashCode % childActorList.size
        log.debug( s"Sending common message to actor with actor index: ${ actorIndex }")
        val actorRef = childActorList( actorIndex )
        actorRef forward msg
      }
    }
    case updateMessage : UpdateMessage => {
      log.debug( "Received update message" )
      val actorIndexOption = for{
        messageId <- updateMessage.msg.map.get( "messageId" )
        agentId <- updateMessage.msg.map.get( "agentId" )
      } yield { MidParsedSourceMessage( agentId, messageId, "").hashCode % childActorList.size  }
      actorIndexOption match{
        case Some( actorIndex ) => {
          log.debug( s"Sending update message to actor with actor index: ${ actorIndex }")
          val actorRef = childActorList( actorIndex )
          actorRef forward updateMessage
        }
        case None => log.debug( "Problem getting message routed to update message")
      }

    }
    case s : SearchIndexedMessage => {
      log.debug( s" Received SearchIndexedMessage: ${s}")
      aggregateCommonMessages(s).pipeTo( sender )
    }
    case i : IndexQuery => {
      log.debug( s" Received IndexQuery")
      aggregateCommonMessages(i).pipeTo( sender )
    }
    case t : TimeLimitedSearchIndexedMessage => {
      log.debug( s" Received TimeLimitedSearchIndexedMessage: ${t}")
      val futureList = childActorList.map{ ref => (ref ? t).mapTo[ CommonMessageList ].fallbackTo( future{ CommonMessageList(Nil) } ) }.toList
      val seqFutureResult = sequence( futureList ).map{ msgList =>
        msgList.foldLeft( CommonMessageResults( CommonMessageList( Nil ))){ ( messageResult, messageList ) =>
          CommonMessageResults( CommonMessageList( messageResult.commonMessageList.messageList ++ messageList.messageList))
        }
      }
      seqFutureResult.pipeTo( sender )
    }
    case c : CountIndexedMessage => {
      log.debug( s"Received CountIndexedMessage: ${c}")
      val futureList = childActorList.map{ ref => ( ref ? c ).mapTo[ Option[ Int ] ].fallbackTo( future( None ) ) }.toList
      sequence( futureList ).map{ f =>
        f.reduce { ( op1, opt2 ) => Option( op1.getOrElse(0) + opt2.getOrElse(0))}
      }.pipeTo( sender )
    }
    case message : MessageHashCode => {
      val index = message.hashCode % childActorList.size
      childActorList( index ).forward( message )
    }
    case x => log.warning( s"Unknown message detected: ${x}. Will ignore.")
  }

  def aggregateCommonMessages[T](  message : T ) : Future[ CommonMessageList] = {
    val futureList = childActorList.map{ ref => (ref ?  message).mapTo[ CommonMessageList ].fallbackTo( future{ CommonMessageList(Nil) } ) }.toList
    sequence( futureList ).map{ msgList =>
      msgList.reduce{ ( msgA, msgB )  =>
        CommonMessageList( msgA.messageList ++ msgB.messageList)
      }
    }
  }
}
