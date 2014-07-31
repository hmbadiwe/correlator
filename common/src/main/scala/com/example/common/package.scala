package com.example


import com.example.common.domain.CommonMessage
import com.example.common.exception.{PhalanxException, MalformedIPException}

import scala.collection.immutable.StringOps


import language.implicitConversions
package object util {
  import org.json4s._

  class CommonMessageDeserializer extends CustomSerializer[ CommonMessage ](
    format =>(
      {
        case JObject( x ) => CommonMessage("fake" -> "data")
      },
      {
        case c : CommonMessage => {
          val fieldList =  c.map.toList.map{ kV =>  JField( kV._1, JString( kV._2 ) ) }
          JObject( fieldList )
        }
      }
      )
  )

  case class CorrelationMessage(map:Map[String,String])
  case class TicketingMessage(map:Map[String,String])
  case class IP(ip: String) extends Serializable

  /** This implicit function convert string to some whenever it finds and assignment from String to Some
    * Exaample: val s: Some[String] = "something special". In general this conversion is not possible without implicit function
    */
  implicit def string2some(str: String): Some[String] = Some(str)

  /** This implicit class is used to perform some additional checks on a value type Int. e.f. whether an in Int is in range */
  implicit class EnrichedInt(segment: Int) {
    def toIPSegment: Int = segment match {
      case x if 0 to 255 contains x => x
      case _                        => throw MalformedIPException("IP segment not in range - check the IP address")
    }
  }

  /** This implicit class provides us the methods that can be performed on String type vals/vars. e.g. we can perform isIP on any arbitrary string */
  implicit class EnrichedString(string: String) {
    /** This uses str2ip implicit function, can be used interchangeably with the exception -
      *  when you have no easy way to assign the string into a types IP value/variable */
    def toIP: IP = string //this assignment causes the call of implicit function str2ip

    /**this import is so far used at only one place so imported locally **/
    import scala.util.control.Exception.catching

    /** This method is implemented with very advanced technique to satisfy very right behaviour. isIP should not throw an exception
      *  even the input pattern does not match - it rather should say yes or no.
      */
    def isIP: Boolean = ({
      catching(classOf[MalformedIPException], classOf[Throwable]).withApply( e => throw new MalformedIPException(s"Your suspicion is valid - '$string' not an IP"))
    } either {
      toIP
    }) match {
      case Right(x) => true
      case Left(x)  => false
    }

    lazy val ArrExp = """\[([0-9\*]+)\]""".r //checks whether a given string is an array representation
    val nfecatching = catching(classOf[NumberFormatException]).withApply(e => throw new PhalanxException("The input may not be a number convertible"))
    def isInt =  (nfecatching either string.trim.toInt) match {
      case Right(x) => true
      case Left(y)  => false
    }
    lazy val matched = (ArrExp findAllIn string).toList
    def isArray: Boolean = matched match {
      case head :: tail => true
      case _            => false
    }
    def arrIndex: Int = matched match {
      case head :: tail   => if( head.trim.equalsIgnoreCase("[all]") || head.trim.equalsIgnoreCase("[*]") )  -9 else head.trim.slice(1, head.trim.length-1).toInt
      case _              => -1
    }
    def arrKey: String = string.slice(0, string.lastIndexOf("["))

    /** I decided not to impliment this method as it may lead to big confusion. Imagine you are checking a value whether that is Boolean and you getting a Boolean answer*/
    //implicit def isBo0lean = ???
  }

  /** This function does similar thing as 'string2some' - it makes assignment possible from String to IP
    *  Example: val ip: IP = "1.1.1.1"
    */
  implicit def str2ip(str: String): IP = {
    try {
      /** Notice an additional method toIPSegment() - this is defined in an implicit class EnrichedInt to ensure valid range */
      str.trim.split('.').toList map (x => x.toInt.toIPSegment) match {
        case one :: two :: three :: four :: Nil => {
          if(one == 0) {
            throw new MalformedIPException(s"Fist element in the IP can not be ZERO")
          } else IP(str)
        }
        case _                                  => throw new MalformedIPException(s"Not an IP Address. This is what I received '$str'. Verify the format. CODE: Sorry, I could not bring a good this time. A catchall match")
      }
    } catch {
      case exc: NumberFormatException => throw new MalformedIPException(s"Not an IP Address. This is what I received '$str'. Verify the format. CODE: $exc" )
      case   _: Throwable             => throw new MalformedIPException(s"Not an IP Address. This is what I received '$str'. Verify the format")
    }
  }

  /** This implicit function tries to convert a String value to a Boolean type when a String value is assigned to a Boolean type */
  implicit def str2bool(string: String): Boolean = string.trim.toLowerCase match {
    case "true"  | "yes"  => true
    case "false" | "no"   => false
    case _                => throw new RuntimeException("String value does not conform to any boolean value like true, false, yes, no")
  }

  /** Handle to application.conf, reference.conf,etc this is to save calling same function over and over again */
  /** This method converts ConfObject to immutable Map */
  /*def conf2map(path: String)(implicit config: com.typesafe.config.Config) : Map[String, String] = {
    val scalaMap: scala.collection.mutable.Map[String,Object] = config.getObject(path).unwrapped
    scalaMap.toMap.mapValues(v=>v.toString).map(e=>e)
  }*/

  def actorURL(actorSystem: String)(implicit config: com.typesafe.config.Config): String = {
    "akka.tcp://" + new StringOps(actorSystem).capitalize + "@" +
      config.getConfig(actorSystem).getString("akka.remote.netty.tcp.hostname") + ":" +
      config.getConfig(actorSystem).getString("akka.remote.netty.tcp.port")
  }

  /** Check for the config hostname/ip and local hostname/ip and if they match then
    * if the config hostname or IP is (localhost or 127.0.0.1 or real (hostname or IP )) create the actor system locally
    */
  def isLocalActorSystem(actorSystem: String)(implicit config: com.typesafe.config.Config) = {
    import java.net._
    config.getConfig(actorSystem).getString("akka.remote.netty.tcp.hostname").trim.toLowerCase match {
      case "localhost" | "127.0.0.1" => true
      case x if x.equalsIgnoreCase(InetAddress.getLocalHost.getHostName) => true
      case x if x.equalsIgnoreCase(InetAddress.getLocalHost.getHostAddress) => true
      case _ => false
    }
  }


}
