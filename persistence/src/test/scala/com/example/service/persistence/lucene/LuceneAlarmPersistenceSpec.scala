package com.cisco.phalanx.persistence.lucene

import com.example.common.domain.CommonMessage
import com.example.persistence.LuceneAlarmPersistence
import com.example.service.SetupDirectory

import org.apache.lucene.store.{Directory, RAMDirectory}

import LuceneAlarmPersistence._
import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers

class LuceneAlarmPersistenceSpec extends FlatSpec with MustMatchers{
  behavior of "Lucene Persistence Service"

  it must "be able to store and retrieve alerts" in new TestLuceneAlarmPersistence {

    saveMessages( testAlerts : _*)

    val wkStationAlarms = persistence findByParameter Map("device_type" -> List( "WK_STATION") )
    wkStationAlarms must have length 3

    val listOfAlarmIdMatches: List[Option[String]] = List(Some("192.168.0.1:8900|1"), Some("192.168.0.10:8900|1"), Some("192.168.0.12:8900|1"))

    val alertIdMatches = wkStationAlarms.filter{ c =>  listOfAlarmIdMatches.contains(c.map.get("alarmId") ) }

    alertIdMatches must have length 3

    val shouldNotMatchAndBeEmpty = wkStationAlarms.filter{ case c:CommonMessage =>
      c.map.get("alarmId") == Some("192.168.0.25:8900|1")
    }
    shouldNotMatchAndBeEmpty must have length 0

  }

  it must "properly retrieve results based on a map of string to list parameters" in new TestLuceneAlarmPersistence {
      saveMessages( testAndOrSearchMessages : _* )
      val searcher = persistence.indexSearcher()

      searcher.search( createBooleanSearchQueryFromMap( testOrSearchParams ), 100 ).scoreDocs must have length( 3 )
      searcher.search( createBooleanSearchQueryFromMap( testAndSearchParams ), 100 ).scoreDocs must have length( 1 )
      searcher.search( createBooleanSearchQueryFromMap( testParamsWithNoResults ), 100 ).scoreDocs must have length( 0 )
      searcher.search( createBooleanSearchQueryFromMap( testAndOrSearchParams ), 100 ).scoreDocs must have length( 2 )
  }
  it must "properly return the correct number of messages found in the index" in new TestLuceneAlarmPersistence {
    saveMessages( testAlerts : _* )
    saveMessages( testAndOrSearchMessages : _*)
    val count = persistence.countByParameter( Map( "device_type" -> List( "WK_STATION", "SERVER", "LAPTOP" ) ) )
    count must be ( 7 )
  }

  it must "properly retrieve stored messages with spaces" in new TestLuceneAlarmPersistence {
    saveMessages( testSearchMessagesWithSpaces : _* )
    persistence.findAllDocuments() must have length ( 3 )

    persistence.findByParameter( Map( "alarmCode" -> List("The quick brown fix had no fleas'"))) must have length(1)
    persistence.findByParameter( Map( "alarmCode" -> List("Carly Simon's on the radio/Neil Simon too!"))) must have length(1)
    persistence.findByParameter( Map( "alarmCode" -> List(" telling the world how you feel "))) must have length(1)
  }

  it must "properly update stored messages" in new TestLuceneAlarmPersistence {
    saveMessages( testSearchMessagesWithSpaces : _*)
    persistence.findAllDocuments() must have length( 3 )

    val newAlarmCode = "The Thriller album, bar none, non-pareil : MJ's greatest work"
    val alarmIdToUpdate =  "test_alarm_2|2"

    persistence.update( CommonMessage( "alarmIdentifier" -> alarmIdToUpdate, "alarmCode" -> newAlarmCode) )
    val allDocuments = persistence.findAllDocuments()
    allDocuments must have length( 3 )

    val updateMessageList = persistence.findByParameter( Map( "alarmIdentifier" -> List(alarmIdToUpdate) ) )

    updateMessageList must have length(1)
    updateMessageList.head.map( "alarmCode") must be( newAlarmCode)

  }

  val testAndOrSearchParams = Map(
    "severity" -> List( "important", "critical"),
    "companyID" -> List( "CPY_ID_0")
  )

  val testOrSearchParams = Map(
    "severity" -> List( "important", "critical", "fatal" )
  )

  val testAndSearchParams = Map(
    "severity"  -> List( "important" ),
    "companyID" -> List( "CPY_ID_0" ),
    "agent_id"  -> List( "qst-splunk-dev"),
    "deviceIP"  -> List( "10.0.0.0" ),
    "messageId" -> List( "test_msg_0" )
  )

  val testParamsWithNoResults = testAndSearchParams ++ Map("randomParam" -> List( "junk" ) )

  val testAlerts = List(
    CommonMessage(Map("alarmId" -> "192.168.0.1:8900|1", "device_type"-> "WK_STATION")),
    CommonMessage(Map("alarmId" -> "192.168.0.10:8900|1", "device_type"-> "WK_STATION")),
    CommonMessage(Map("alarmId" -> "192.168.0.12:8900|1", "device_type"-> "WK_STATION")),
    CommonMessage(Map("alarmId" -> "192.168.0.25:8900|1", "device_type"-> "NETWORK"))
  )

  val testAndOrSearchMessages = List(
    CommonMessage( "severity" -> "important"                , "companyID" -> "CPY_ID_0", "agent_id" -> "qst-splunk-dev"   , "deviceIP" -> "10.0.0.0", "messageId" -> "test_msg_0", "device_type"-> "SERVER"),
    CommonMessage( "severity" -> "critical"                 , "companyID" -> "CPY_ID_0", "agent_id" -> "qst-splunk-prod"  , "deviceIP" -> "10.0.0.1", "messageId" -> "test_msg_1", "device_type"-> "LAPTOP"),
    CommonMessage( "severity" -> "fatal"                    , "companyID" -> "CPY_ID_1", "agent_id" -> "qst-splunk-dev"   , "deviceIP" -> "10.0.0.2", "messageId" -> "test_msg_2", "device_type"-> "SERVER"),
    CommonMessage( "severity" -> "we_re_all_going_to_die!!!", "companyID" -> "CPY_ID_2", "agent_id" -> "qst-splunk-it"    , "deviceIP" -> "10.0.0.3", "messageId" -> "test_msg_3", "device_type"-> "SERVER")
  )

  val testSearchMessagesWithSpaces = List(
    CommonMessage( "alarmCode" -> "The quick brown fix had no fleas'", "device_id" -> "10.0.0.1 2523", "alarmIdentifier" ->"test_alarm_1|1"),
    CommonMessage( "alarmCode" -> "Carly Simon's on the radio/Neil Simon too!", "device_id" -> "10.0.0.1 2522", "alarmIdentifier" ->"test_alarm_2|2"),
    CommonMessage( "alarmCode" -> " telling the world how you feel ", "alarmIdentifier" ->"test_alarm_3|3")
  )

  val testSearchMessagesWithTimeRange = List(
    CommonMessage( "alarmId" -> "192.168.0.1:8900|1", "device_type"-> "WK_STATION", "timeStamp" -> "1378869163" ),
    CommonMessage( "alarmId" -> "192.168.0.10:8900|1", "device_type"-> "WK_STATION" , "timeStamp" -> "1378869163"),
    CommonMessage( "alarmId" -> "192.168.0.12:8900|1", "device_type"-> "WK_STATION", "timeStamp" -> "1378869163"),
    CommonMessage( "alarmId" -> "192.168.0.25:8900|1", "device_type"-> "NETWORK", "timeStamp" -> "1378869163")
  )


}

trait TestLuceneAlarmPersistence {
  val persistence = new LuceneAlarmPersistence with SetupDirectory{
    val directory : Directory =  setupDirectory(new RAMDirectory())
  }

  def saveMessages ( msgs : CommonMessage *) {
    msgs.foreach{  msg =>  persistence.save( msg )  }
  }
}
