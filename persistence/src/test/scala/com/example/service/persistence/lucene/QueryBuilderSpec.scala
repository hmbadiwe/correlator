package com.cisco.phalanx.persistence.lucene

import com.example.common.domain.CommonMessage
import com.example.persistence.lucene.QueryBuilder
import org.scalatest.matchers.MustMatchers
import org.scalatest.FlatSpec



class QueryBuilderSpec extends FlatSpec with MustMatchers with QueryBuilderSpecData{
  it must "create a simple BOOLEAN 'and' query based on parameters" in {
      val query = QueryBuilder.generateQuery( testAndQueryMessage )
      query must be ( "(deviceIP:\"10.0.0.0\" AND searchId:\"test_search_0\" AND alarmType:\"hw_alarm\")" )
  }

  it must "create a simple boolean 'AND' query with a time based parameter" in {
    val query = QueryBuilder.generateQuery( testTimedQueryMessage )
    query must be ("(searchField:\"the quick brown fox\" AND deviceIP:\"10.0.0.1\" AND timeStamp:[1378871319 TO 1378871334])")

  }
  it must "create a combination of 'OR' queries" in {
    val query = QueryBuilder.generateQuery( testOrQueryMessage)
    query must be ("(deviceIP:\"10.0.0.0\") OR (searchId:\"test_search_0\") OR (alarmType:\"hw_alarm\")")
  }

  it must "create a combination of 'OR' and 'AND' queries with 'AND' taking precedence" in {
    val query = QueryBuilder.generateQuery(testAndOrQueryMessage)
    query must be ("(deviceIP:\"10.0.0.0\" AND searchId:\"test_search_0\") OR (alarmType:\"hw_alarm\")")
  }
  it must "create an empty string with no correlation level extant" in{
    val query = QueryBuilder.generateQuery(testMessageWithoutCorrelationLevel)
    query must be ('empty)
  }
  it must "create a query with missing correlation field values" in {
     val query = QueryBuilder.generateQuery( testMessageWithoutCorrelationValues )
     query must be ('empty)
  }

}

trait QueryBuilderSpecData{
   val testAndQueryMessage = CommonMessage(
    "deviceIP" -> "10.0.0.0",
    "searchId" -> "test_search_0",
    "alarmType" -> "hw_alarm",
    "correlationLevel" -> "deviceIP/searchId/alarmType"
   )

  val testTimedQueryMessage = CommonMessage(
    "deviceIP" -> "10.0.0.1",
    "searchField" -> "the quick brown fox",
    "alarmType" -> "hw_alarm",
    "timeStamp" -> "1378871334",
    "correlationLevel" -> "searchField/deviceIP/timeStamp<>(-15,0)"
  )

  val testOrQueryMessage = CommonMessage(
    "deviceIP" -> "10.0.0.0",
    "searchId" -> "test_search_0",
    "alarmType" -> "hw_alarm",
    "correlationLevel" -> "deviceIP|searchId|alarmType"
  )

  val testAndOrQueryMessage = CommonMessage(
    "deviceIP" -> "10.0.0.0",
    "searchId" -> "test_search_0",
    "alarmType" -> "hw_alarm",
    "correlationLevel" -> "deviceIP/searchId|alarmType"
  )
  val testMessageWithoutCorrelationLevel = CommonMessage(
    "deviceIP" -> "10.0.0.0",
    "searchId" -> "test_search_0",
    "alarmType" -> "hw_alarm"
  )
  val testMessageWithoutCorrelationValues = CommonMessage(
    "deviceIP" -> "10.0.0.0",
    "searchId" -> "test_search_0",
    "alarmType" -> "hw_alarm",
    "correlationLevel" ->"alarmCode/timeStamp<>(0,15)|ipAddress"
  )
}
