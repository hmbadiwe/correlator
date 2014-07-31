package com.example.actor

import akka.actor.{Props, Actor, ActorLogging}
import akka.routing.RoundRobinRouter
import com.example.common.domain.{MidParsedSourceMessage, CommonMessage, UpdateMessage}
import org.apache.lucene.store.Directory

class PersistenceIODirectorActor( numberOfReadRoutees : Int, _directory : Directory ) extends Actor with ActorLogging {


  override def preStart() {
    super.preStart()
    log.debug( s"Starting PersistenceIODirectorActor on path: ${self.path}" )
  }

  val writeActor = context.actorOf( Props( new PersistenceActor {
    val directory: Directory = _directory
  } ) , "writer" )

  val readActors = context.actorOf( Props( new PersistenceActor {
    val directory: Directory = _directory
  }  ).withRouter( RoundRobinRouter( nrOfInstances = numberOfReadRoutees ) ) )



  def receive = {
    case updateMsg : UpdateMessage => writeActor ! updateMsg
    case c : CommonMessage => writeActor ! c
    case m : MidParsedSourceMessage =>  writeActor ! m

    case readMessage => {
       log.debug( s"Forward message of type ${ readMessage.getClass }")
        readActors forward readMessage
    }
  }
}
