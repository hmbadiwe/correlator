package com.example.service.correlation.service

import com.example.common.domain.CommonMessage
import com.example.persistence.LuceneAlarmPersistence
import com.example.service.SetupDirectory
import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers
import com.example.service.correlation.{CorrelationService, TestCorrelationData}
import org.apache.lucene.store.{Directory, RAMDirectory}





class CorrelationServiceSpec extends FlatSpec with MustMatchers with TestCorrelationData {

  trait TestCorrelationService extends SetupDirectory {
    val testPersistenceService = new LuceneAlarmPersistence {
      val directory: Directory = setupDirectory( new RAMDirectory() )
    }
    implicit val correlationService = new CorrelationService(){
      val correlatedId = "correlatedId"
      val uniqueId = "alarmIdentifier"
      val timeStampField = "firstOccurrence"
      val persistenceService = testPersistenceService
    }
    def findAllDocuments = testPersistenceService.findAllDocuments()
    def saveMessages( messages : CommonMessage*  ) {
      messages.foreach{ c => testPersistenceService.save( c ) }
    }
  }

  behavior of "Correlation Service"

  def mustBeRoot( msg : CommonMessage)(implicit correlationService : CorrelationService){
    msg.get( correlationService.correlatedId) must be( msg.get( correlationService.uniqueId) )
  }
  def mustNotBeRoot( msg : CommonMessage )(implicit correlationService : CorrelationService ){
    msg.get( correlationService.correlatedId) must not be( msg.get( correlationService.uniqueId) )
  }

  it must "create a new root alarm when the first alarm is received" in new TestCorrelationService {
    val addResult = correlationService.correlateAlarm( testAlarms("first"))
    mustBeRoot( addResult )
  }

  it must "properly create a new root alarm when a non correlate-able alarm is received" in new TestCorrelationService{

    val firstResult = correlationService.correlateAlarm( testAlarms("first") )

    val secondResult = correlationService.correlateAlarm( testAlarms( "second_non_correlated") )

    mustBeRoot( firstResult )

    mustBeRoot( secondResult )

    findAllDocuments must have length( 2 )
  }

  it must "properly correlate alarm when a correlate-able alarm is received" in new TestCorrelationService{
    import correlationService._
    val firstResult = correlateAlarm( testAlarms("first") )

    val secondResult = correlateAlarm( testAlarms( "second_correlated" ) )
    mustBeRoot( firstResult )

    secondResult.map.get( correlatedId ) must be ( firstResult.map.get( uniqueId ) )

    val thirdResult = correlateAlarm( testAlarms( "third_correlated"))

    thirdResult.map.get( correlatedId ) must be ( firstResult.map.get( uniqueId ) )

    val alarmsInResultSet = findAllDocuments

    alarmsInResultSet must have length( 3 )
  }

  it must "properly correlate an alarm on multiple fields" in new TestCorrelationService{
    val firstResult = correlationService.correlateAlarm( testAlarms( "first_multi_correlated" ) )
    val secondResult = correlationService.correlateAlarm( testAlarms( "second_multi_correlated") )
    val thirdResult = correlationService.correlateAlarm( testAlarms( "third_multi_correlated") )
    val fourthResult = correlationService.correlateAlarm( testAlarms( "fourth_multi_correlated") )
    val fifthResult = correlationService.correlateAlarm( testAlarms( "fifth_multi_correlated") )
    val sixthResult = correlationService.correlateAlarm( testAlarms( "second_multi_non_correlated") )

    mustBeRoot( firstResult )
    secondResult.map.get( "correlatedId") must equal ( firstResult.map.get( "alarmIdentifier" ) )
    thirdResult.map.get( "correlatedId") must equal ( firstResult.map.get( "alarmIdentifier" ) )
    fourthResult.map.get( "correlatedId") must equal ( firstResult.map.get( "alarmIdentifier" ) )
    fifthResult.map.get( "correlatedId") must equal ( firstResult.map.get( "alarmIdentifier" ) )
    mustBeRoot( sixthResult )
    findAllDocuments must have length( 6 )

  }

  it must "not correlate an alarm on multiple fields if the fields do not match" in new TestCorrelationService {
    correlationService.correlateAlarm( testAlarms( "first_multi_correlated" ) )

    val secondResult = correlationService.correlateAlarm( testAlarms( "second_multi_non_correlated"))
    mustBeRoot( secondResult )

  }

  it must "return a map of correlate-able fields" in {
    val map =  CorrelationService.findParametersFromMessage( testAlarms( "full_multi_correlated" ) )

    map must have size ( 3 )

    map must be( Map( "device_id" -> List( "test_device_id" ), "alarm_id" -> List( "test_alarm_id" ), "ip_address" -> List( "10.0.0.1" ) ) )

  }
  it must "set the root_cause if there are no correlate-able fields" in new TestCorrelationService{
    correlationService.correlateAlarm( testAlarms( "first") )

    val secondAlarm = correlationService.correlateAlarm( testAlarms("second_no_correlatable_fields"))
    mustBeRoot( secondAlarm )

    val thirdAlarm = correlationService.correlateAlarm( testAlarms( "third_no_correlatable_values"))
    mustBeRoot( thirdAlarm )
  }

  it must "correlate to an id when all the messages with correlated id have the same correlated id" in new TestCorrelationService {
    import correlationService._
    testMessagesWithSameCorrelationIds.foreach{ t => testPersistenceService.save( t ) }
    val correlatedAlarm = correlateAlarm( testAlarms( "first" ) )
    correlatedAlarm.map.get( correlatedId ) must be ( testMessagesWithSameCorrelationIds.head.map.get( correlatedId ) )
  }

  it must "correlate to the oldest message with a correlated id when there are messages with different correlated ids that aren't part of root messages" in new TestCorrelationService {
    import correlationService._
    saveMessages( testMessagesWithSameCorrelationIds :_*)
    saveMessages( testMessagesWithDifferentCorrelationIds :_*)
    val correlatedAlarm = correlateAlarm( testAlarms( "first" ) )
    correlatedAlarm.map.get( correlatedId ) must be ( Some( "test_agent_id_3|3" ) )
  }

  it must "correlate to the correlated id that is the root alarm" in new TestCorrelationService {
     import correlationService._
    saveMessages( testMessagesWithDifferentCorrelationIdsWithRoot :_*)
    val correlatedAlarm = correlateAlarm( testAlarms( "first"))
    correlatedAlarm.map.get( correlatedId ) must be ( Some( "test_agent_id_8|8" ) )
  }
  it must "correlate to the correlated id that is the oldest root alarm" in new TestCorrelationService {
    import correlationService._
    saveMessages( testMessagesWithDifferentCorrelationIdsWithRoot :_* )
    saveMessages( testMessagesWithDifferentCorrelationIdsWithMultipleRoots :_*)
    val correlatedAlarm = correlateAlarm( testAlarms( "first"))
    correlatedAlarm.map.get( correlatedId ) must be ( Some( "test_agent_id_12|12" ) )
  }

  it must "correlate alarms based on time" in new TestCorrelationService {
    import correlationService._

    val firstCorrelatedAlarm = correlateAlarm( testMessageWithTimeCorrelation(0) )
    mustBeRoot( firstCorrelatedAlarm )

    val secondCorrelatedAlarm = correlateAlarm( testMessageWithTimeCorrelation(1))
    secondCorrelatedAlarm( correlatedId ) must be ( firstCorrelatedAlarm( uniqueId ) )


    val thirdCorrelatedAlarm = correlateAlarm( testMessageWithTimeCorrelation(2))
    thirdCorrelatedAlarm( correlatedId ) must be ( firstCorrelatedAlarm( uniqueId ) )

    val fourthCorrelatedAlarm = correlateAlarm( testMessageWithTimeCorrelation(3))
    mustBeRoot( fourthCorrelatedAlarm )


  }
}
