package com.example.actor

import akka.actor.{ActorLogging, Actor}
import com.cisco.phalanx.persistence.lucene.LuceneAlarmPersistence
import org.joda.time.format.ISODateTimeFormat
import org.apache.lucene.store.Directory
import scala.compat.Platform
import org.apache.lucene.search.{MatchAllDocsQuery, SortField, Sort}
import org.apache.lucene.index.DirectoryReader
import com.example.persistence.LuceneAlarmPersistence
import com.example.domain._
import com.example.domain.MidParsedSourceMessage
import com.example.domain.SearchIndexedMessage
import com.example.domain.FindIndexedMessage

/**
 * Created with IntelliJ IDEA.
 * User: hmbadiwe
 * Date: 3/1/14
 * Time: 8:51 PM
 * To change this template use File | Settings | File Templates.
 */
trait PersistenceActor extends Actor with ActorLogging with LuceneAlarmPersistence{

  val maxNumberOfDocs = 500
  lazy val dateTimeFormatter = ISODateTimeFormat.dateHourMinuteSecond()
  override val defaultSearchString: String = "messageId"

  override def preStart() {
    super.preStart()
    log.info(s"Starting restful actor with path: ${self.path}")
  }

  val directory: Directory

  def receive = {
    case c: CommonMessage => {
      log.debug( s"Received CommonMessage  on actor path: ${self.path}" )
      save( c )
      log.debug( s"Saved CommonMessage on actor path: ${self.path}" )
    }
    case MidParsedSourceMessage( agentId, messageId, message ) => {
      log.debug(s"Mid Parsed message received")
      save( CommonMessage( "mid" -> s"${agentId}|${messageId}", "message" -> message, "timeStamp" -> Platform.currentTime.toString ) )
      log.debug(s"SaveSuccess with agentId: ${agentId} and messageId: ${messageId}")
    }
    case f @ FindIndexedMessage( agentId, messageId ) => {
      log.debug(s"Received FindIndexedMessage: ${f}")
      val messageList = findIndexedMessage(agentId, messageId)
      sender ! messageList.headOption
    }
    case s: SearchIndexedMessage => {
      log.debug( s"SearchIndexedMessage received: ${s} on actor path: ${self.path}")
      val searchResultList =  findByParameter(s.messageMap)
      log.debug( s"Found result of size: ${searchResultList.size} on actor path: ${ self.path}" )
      sender ! CommonMessageList( searchResultList )
    }
    case t : TimeLimitedSearchIndexedMessage => {
      log.debug( s"TimeLimited search handled in ${Thread.currentThread().getName}")
      log.debug( s"TimeLimitedSearchIndexedMessage")
      val searchResultList = findIndexedMessageSortAndLimitByTime( t.messageMap, t.limit )
      log.debug( s"Found result of size: ${searchResultList.size} on actor path: ${ self.path}" )
      sender ! CommonMessageList( searchResultList )
    }
    case count : CountIndexedMessage => {
      log.debug( s"CountIndexedMessage received: ${count}")
      sender ! Option( countByParameter( count.messageMap ) )
    }

    case updateMsg : UpdateMessage => {
      log.debug( "Update message received")
      update( updateMsg.msg )
    }

    case i : IndexQuery => {
      log.debug( "Indexed query received")
      sender ! query( i.queryString )
    }

  }

  def findIndexedMessage(agentId: String, messageId: String ) : List[ CommonMessage ] = {
    log.debug( s"Finding message with agentId ${agentId} and messageId ${messageId}" )
    findByParameter( Map( "mid" -> List( s"${agentId}|${messageId}" ) ) )
  }

  def findIndexedMessageSortAndLimitByTime( map : Map[ String, List[ String ]], limit : Option[ Int ] ) : List[ CommonMessage ] = {
    searchOperation { searcher =>
      val topDocs = searcher.search( createQuery( map ), limit.getOrElse(100), new Sort( new SortField( "firstOccurrence", SortField.Type.STRING ) ) )
      LuceneAlarmPersistence.convertDocsToMessageList( topDocs, searcher )
    }
  }

  def documentCount() : Int = {
    def indexReader = DirectoryReader.open( directory )
    val numDocs = indexReader.numDocs()
    indexReader.close()
    numDocs
  }


  def listOfAllMids: List[ String ] = {

    val  numberDocsToRetrieve = math.min(indexSearcher.getIndexReader.maxDoc(), maxNumberOfDocs)
    val query = new MatchAllDocsQuery
    val topDocs = indexSearcher.search(query, numberDocsToRetrieve)
    val sortedTuples = topDocs.scoreDocs.map{ scoreDoc=>
      val doc = indexSearcher.doc(scoreDoc.doc)
      (Option(doc.get("mid")), Option(doc.get("timeStamp")))
    } sortBy{ elem => elem._2.getOrElse("")} toList

    sortedTuples.map{ tuple => s"${tuple._1.getOrElse("")}  ${ tuple._2.map(_.toLong).map{ timeStamp => dateTimeFormatter.print(timeStamp)  }.getOrElse("") }"  }
  }

}
