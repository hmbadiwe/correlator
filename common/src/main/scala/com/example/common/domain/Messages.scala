package com.example.common.domain

import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: hmbadiwe
 * Date: 3/1/14
 * Time: 8:41 PM
 * To change this template use File | Settings | File Templates.
 */
case class CommonMessage( map : Map[String, String]){
  override def hashCode = UUID.randomUUID().hashCode()
  def apply( key : String ) : String = map(key)
  def get( key : String ) : Option[ String ] = map.get(key)
}
case class SourceMessage( string : String )
case class CommonMessageList( messageList: List[ CommonMessage] )
object CommonMessage{
  def apply( tupleList: ( String, String )* ) : CommonMessage = CommonMessage( tupleList.toMap )

}
object CommonMessageList{
  def apply( messages : CommonMessage*) : CommonMessageList = CommonMessageList( messages.toList )
}
trait MessageHashCode{
  val agentId: String
  val messageId: String
  override def hashCode: Int = scala.math.abs( ( agentId + messageId ).hashCode )
}

object Create {
  def apply( message : CommonMessage ) : Create = Create( List( message ) )
  def apply( messageList : CommonMessageList ) : Create = Create( messageList.messageList )
}


case class MidParsedSourceMessage( agentId: String, messageId: String, message: String ) extends MessageHashCode

case class FindIndexedMessage( agentId: String, messageId: String) extends MessageHashCode

case class SearchIndexedMessage(  messageMap: Map[ String, List[ String ] ], limit : Option[ Int ] = None )

case class IndexQuery( queryString : String )

case class TimeLimitedSearchIndexedMessage(  messageMap: Map[ String, List[ String ] ], limit : Option[ Int ] = None )

case class SaveSuccess(agentId: String, messageId: String)

case class RequestAllMids(index:Int)

case class CountIndexedMessage( messageMap : Map[ String, List[ String ] ] )

case class UpdateMessage( msg : CommonMessage )

case class CommonMessageResults( commonMessageList : CommonMessageList )

case class Create(message: List[CommonMessage])
