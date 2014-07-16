package com.example.service.correlation

import akka.actor._
import akka.kernel._
import com.typesafe.config.ConfigFactory

class IncidentPing extends Bootable {
  println("anything going on here?")

  def startup() = {}

  def shutdown() = {}

  val system = ActorSystem("MySystem", ConfigFactory.load.getConfig("rulerunner"))
  val ruleRunner = system.actorOf(Props[CorrelationActor], name = "RuleRunner")
  ruleRunner ! Incident(6, System.currentTimeMillis()+4)
  ruleRunner ! Incident(6, System.currentTimeMillis()+5)
  ruleRunner ! Incident(7, System.currentTimeMillis()+6)
}