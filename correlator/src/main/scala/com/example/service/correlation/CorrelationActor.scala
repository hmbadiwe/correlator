package com.example.service.correlation

import akka.actor._
import com.example.common.domain.{Create, CommonMessageList}

import com.typesafe.config.ConfigFactory
import com.example.service.correlation.persistence.CorrelationPersistenceService
import com.example.util._


class CorrelationActor( persistenceServiceActor : ActorRef ) extends Actor with ActorLogging with CorrelationService{

  implicit val config = ConfigFactory.load()
  val correlatedId = "correlatedId"
  val timeStampField = "firstOccurrence"
  val uniqueId = "alarmIdentifier"

  val correlationPersistenceActorPath = actorURL("CorrelationActorSystem") + "/user/persistence"

  val persistenceService = new CorrelationPersistenceService( persistenceServiceActor )
    /** TODO: When correlation actor returns specific message like correlated message,
    * then the Ticketing actor will be enable to process correlated message
    */
  val ticketingActor = context.actorSelection( actorURL( "TicketingActorSystem" ) + "/user/ticketingActor")
  implicit val system = context.system
  def receive = {
    case commonMessageList : CommonMessageList => {
      logger.info( s"CommonMessageList of size ${ commonMessageList.messageList.size } received")
      commonMessageList.messageList.foreach { message =>
       val correlatedAlarm = correlateAlarm( message )
       //GraphPoster( "Correlation", correlatedAlarm )
       val createMsg = Create( correlatedAlarm )
       logger.debug( "Correlation Actor sending message to ticketing")
       ticketingActor ! createMsg
      }
    }
    case x => logger.info(s"Unknown message received: ${x}")
  }

}

object Runner extends App {
  println("anything going on here?")
  val system = ActorSystem("MySystem", ConfigFactory.load.getConfig("rulerunner"))
  val ruleRunner = system.actorOf(Props[CorrelationActor], name = "RuleRunner")
  ruleRunner ! Incident(6, System.currentTimeMillis()+4)
  ruleRunner ! Incident(6, System.currentTimeMillis()+5)
  ruleRunner ! Incident(7, System.currentTimeMillis()+6)
}