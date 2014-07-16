package com.example.service.correlation

import akka.actor.{ Props, ActorSystem}
import akka.kernel.Bootable
import akka.routing.RoundRobinRouter
import com.typesafe.config.ConfigFactory
import com.example.service.persistence.actor.{PersistenceIODirectorActor, PersistenceRouterActor}
import com.example.service.persistence.lucene.SetupDirectory


class CorrelationActorSystem extends Bootable with SetupDirectory{
  val config = ConfigFactory.load().getConfig("CorrelationActorSystem")
  val system = ActorSystem("CorrelationActorSystem", config )

  val actorPrefixName = config.getString( "indexActorName" )
  val childActorCount = config.getInt( "numberOfActorRoutees" )
  val readActorCountPerInstance = config.getInt( "numberOfReadingActorRoutees")
  val directoryBasePath = config.getString("luceneDirectory")

  val childActors = directoryList( childActorCount, Option( directoryBasePath ) ).map{ directory => system.actorOf( Props( new PersistenceIODirectorActor( readActorCountPerInstance, directory ) ) ) }
  val persistenceRouterActor = system.actorOf( Props( new PersistenceRouterActor( childActors ) ).withRouter( RoundRobinRouter(nrOfInstances = config.getInt("persistenceActorInstance"))), "persistence")
  val correlationActor = system.actorOf (Props( new CorrelationActor( persistenceRouterActor ) ).withRouter( RoundRobinRouter( nrOfInstances = config.getString( "correlationActorInstance" ).toInt ) ), "correlation" )

  def startup() = {}

  def shutdown(){
    system shutdown
  }
}


