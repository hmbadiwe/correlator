package com.example.service.correlation

import com.example.service.correlation.CorrelationService.findParametersFromMessage
import org.slf4j.LoggerFactory

trait CorrelationService{
  val logger = LoggerFactory.getLogger(getClass )
  val persistenceService : AlarmPersistence
  val uniqueId : String
  val correlatedId : String
  val timeStampField : String

  def makeRoot( message : CommonMessage ) : CommonMessage = CommonMessage( message.map ++ Map( correlatedId -> message.map( uniqueId ) ) )

  def correlateAlarm( message: CommonMessage ) :  CommonMessage  = {
    val fieldMap = findParametersFromMessage( message )
    val rootMessage = makeRoot(message)
    persistenceService.save( rootMessage )
    val correlatedMessage = {
      if( fieldMap.isEmpty) rootMessage
      else{
          logger.debug( s"searching persistence service for fieldMap: ${ fieldMap }")
          val listMessages = persistenceService.findByParameter( fieldMap )
          logger.debug( s"  found messages of size: ${ listMessages.size }")
          if( listMessages == Nil ) rootMessage //shouldn't happen
          else{
            val filterMessages = listMessages.filter( c =>  c.map.get( uniqueId ) != message.map.get( uniqueId ) )
            if( filterMessages == Nil)  rootMessage
            else{
              def timeStampFilter( c : CommonMessage ) =  c.map.get( timeStampField ).getOrElse("")
              // find a correlation id
              val correlatedIdTimestampTuple = filterMessages.map{ c =>
                for{
                  cc <- c.map.get( correlatedId )
                  tt <- c.map.get( timeStampField )
                  id <- c.map.get( uniqueId )
                }yield( ( cc, tt, id ))
              }.flatten      // list of ( correlatedId, firstOccurrence, uniqueId or alarmIdentifier ) tuples

              if( correlatedIdTimestampTuple == Nil){ //No correlation ids, it should have been saved before
                val possibleRootCause =  filterMessages.sortBy( timeStampFilter ).head
                CommonMessage( message.map ++ Map( correlatedId -> possibleRootCause.map( uniqueId ) ) )
              }
              else  {
                val correlatedSet = correlatedIdTimestampTuple.map{ correlatedIdTimestamp => correlatedIdTimestamp._1 }.toSet // unique correlatedIds
                val messageToBeUpdated = {
                  val correlatedIdToUse =  {
                    if( correlatedSet.size == 1 ) correlatedSet.head
                    else{
                      //find the correlated it that's a root cause
                      val correlatedIdRootCauseTuples = correlatedIdTimestampTuple.filter{ t =>
                        val tupleCorrelatedId = t._1
                        val tupleUniqueId = t._3
                        tupleCorrelatedId == tupleUniqueId
                      }.sortBy( cTuple => cTuple._2 ) //sort by the timestamp
                      // if none are found
                      if( correlatedIdRootCauseTuples == Nil ) { // Use the oldest correlated it
                        val oldestCorrelatedTimestampTuple = correlatedIdTimestampTuple.sortBy( correlatedIdTimestamp => correlatedIdTimestamp._2 ).head
                        oldestCorrelatedTimestampTuple._1
                      }
                      else correlatedIdRootCauseTuples.head._1
                    }
                  }
                  CommonMessage( message.map ++ Map( correlatedId -> correlatedIdToUse ) )
                }
                persistenceService.update( messageToBeUpdated )
                messageToBeUpdated
              }
            }
          }
        }
      }
    correlatedMessage
  }
}

object CorrelationService {
  val fieldDelimiter = "/"
  val correlationField = "correlationLevel"


  def findParametersFromMessage( message: CommonMessage ): Map[ String, List[ String ] ] = {
    message.map.get(correlationField).map{ s =>
      val correlationFields = s.split(fieldDelimiter).toList
      message.map.filterKeys{ key => correlationFields.contains( key )  } map{ kV => ( kV._1, List( kV._2 ) ) }
    }.getOrElse( Map.empty[ String, List[ String ] ])
  }

  def addCorrelation( message : CommonMessage, correlationId : String ) : CommonMessage = {
    val augmentedMap = message.map ++ Map( "correlatedId" -> correlationId)
    message.copy( map = augmentedMap)
  }

  def makeMessageRootCause( message: CommonMessage ) : CommonMessage =  CommonMessage( message.map ++ Map("root_cause" -> "true") )

}
