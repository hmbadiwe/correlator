package com.example.persistence

import com.example.common.domain.CommonMessage
import org.apache.lucene.search._
import org.apache.lucene.index._
import org.apache.lucene.document.{Field, StringField, Document}
import org.apache.lucene.store.Directory
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.queryparser.classic.QueryParser
import scala.collection.JavaConverters._

/**
 * Created with IntelliJ IDEA.
 * User: hmbadiwe
 * Date: 3/1/14
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */



trait AlarmPersistence {
  def save(alarm:CommonMessage)
  def findByParameter( params : Map[ String, List[ String ] ] ) : List[ CommonMessage ]
  def update( alarm : CommonMessage )
  def query( queryString : String ) : List[ CommonMessage ]
}

object LuceneAlarmPersistence{

  def createBooleanSearchQueryFromMap( params : Map[ String, List[ String ] ] ) : Query = {

    import org.apache.lucene.search.BooleanClause.Occur._
    val aggregateQuery = new BooleanQuery()

    params.foldLeft( aggregateQuery ){ ( query, queryParamTuple) =>
      val paramKey = queryParamTuple._1
      val paramValues = queryParamTuple._2

      val orQuery = new BooleanQuery()
      paramValues.foreach { paramValue => orQuery.add( new TermQuery( new Term(  paramKey , paramValue  ) ), SHOULD)  }
      query.add( orQuery, MUST )
      query
    }

    aggregateQuery
  }
  def convertDocsToMessageList( topDocs : TopDocs, searcher : IndexSearcher ) : List[ CommonMessage ] = {
    topDocs.scoreDocs.map { scoreDoc =>
      val doc = searcher.doc( scoreDoc.doc)
      val fields = doc.getFields.asScala
      val msgMap= fields.foldLeft( List.empty[ ( String,String ) ] ) { (accumList, field) =>
        accumList ++   Map( field.name() -> doc.get( field.name()) )
      }.toMap
      CommonMessage( msgMap )
    }.toList
  }

  def convertMessageToDoc( message : CommonMessage ) : Document = {
    val alertDoc = new Document
    message.map.foreach{
      kV =>
        alertDoc.add( new StringField(  kV._1, kV._2 , Field.Store.YES ) )
    }
    alertDoc
  }
}

trait LucenePersistence extends AlarmPersistence {
  import LuceneAlarmPersistence._

  val defaultSearchString:String

  val directory:Directory

  val maxCount = 500

  val uniqueId : String


  lazy val analyzer = new KeywordAnalyzer

  def indexWriter() = {

    val iwConfig: IndexWriterConfig = new IndexWriterConfig(
      Version.LUCENE_43,
      analyzer
    )
    iwConfig.setOpenMode(IndexWriterConfig.OpenMode.APPEND)
    val iw = new IndexWriter(
      directory,
      iwConfig
    )
    iw
  }

  def indexSearcher() = {
    new IndexSearcher( indexReader )
  }
  def indexSearcher( indexReader: IndexReader) = new IndexSearcher( indexReader )

  def indexReader() = DirectoryReader.open( directory )

  def save( message: CommonMessage ) =    writeOperation { writer =>  writer.addDocument( convertMessageToDoc( message  ) ) }

  def searchOperation[ T ]( fn : IndexSearcher => T ) : T = {
    val reader = indexReader()
    implicit val searcher = indexSearcher( reader )
    val result = fn( searcher )
    reader.close()
    result
  }

  def update( message : CommonMessage ) {
    searchOperation { searcher =>
      val topDocs = searcher.search( new TermQuery( new Term( uniqueId, message.map( uniqueId)) ), 1 )
      if( topDocs.totalHits == 0 ){ save( message )     }
      else {
        writeOperation{ writer =>   writer.updateDocument(  new Term( uniqueId, message.map( uniqueId) ) , convertMessageToDoc( message) )   }
      }
    }
  }

  def writeOperation( fn : IndexWriter => Unit ) {
    val writer = indexWriter()
    fn( writer )
    writer.close()
  }
  def countByParameter(  params : Map[ String, List[ String ]]) : Int = {
    val reader = indexReader()
    val searcher = indexSearcher( reader )
    val topDocs = findDocs( searcher, params, Int.MaxValue  )
    topDocs.totalHits
  }
  private def findDocs( searcher : IndexSearcher, params : Map[ String, List[ String ]], maxCount : Int ) : TopDocs = {
    searcher.search( createQuery( params ), maxCount)
  }

  def createQuery( params : Map[ String, List[ String ] ] ) : Query = createBooleanSearchQueryFromMap( params )


  def findByParameter( params : Map [ String, List[ String ] ] ): List[ CommonMessage ] = {
    searchOperation { searcher =>
      val topDocs = findDocs(  searcher, params, maxCount )
      val msgList = convertDocsToMessageList( topDocs, searcher )
      msgList
    }
  }
  def findByParameters( params : (String, List[String])* ) = findByParameter( params.toMap )

  def findAllDocuments( limit : Option[ Int ] = None ) : List[ CommonMessage ] = {
    searchOperation { searcher =>
      val topDocs = searcher.search( new MatchAllDocsQuery(), limit.getOrElse( Int.MaxValue ) )
      convertDocsToMessageList( topDocs, searcher )
    }
  }
  def query( queryString : String ) : List[ CommonMessage ] = {
    searchOperation{ searcher =>
    // change to ConstantScoreQuery. See if this improves performance
      val query = new QueryParser( Version.LUCENE_43, uniqueId, analyzer).parse( queryString )
      val topDocs = searcher.search( query, 500 )
      convertDocsToMessageList( topDocs, searcher )
    }
  }
}

trait LuceneAlarmPersistence extends LucenePersistence{
  val defaultSearchString:String = "messageId"
  override val uniqueId = "alarmIdentifier"
  //def findByParameter( params : Map[ String, String ]) = findByParameter( params.map{ p => ( p._1, List( p._2 ) ) } )
}
