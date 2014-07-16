package com.example.utils

import org.json4s._
import org.json4s.JsonMethods._
import com.example.domain.{CommonMessageList, MidParsedSourceMessage}

/**
 * Created with IntelliJ IDEA.
 * User: hmbadiwe
 * Date: 3/1/14
 * Time: 9:18 PM
 * To change this template use File | Settings | File Templates.
 */
object JsonParserUtils {
  implicit class LiftJValueWithFilter(self: JValue)
    extends JValueWithFilter(self, _ => true)
  class JValueWithFilter(self: JValue, p: JValue => Boolean) {
    def map[T](f: JValue => T): List[T] =
      self.filter(p).map(f)
    def flatMap[T](f: JValue => List[T]): List[T] =
      self.filter(p).flatMap(f)
    def foreach(f: JValue => Unit): Unit =
      self.filter(p).foreach(f)
    def withFilter(q: JValue => Boolean): JValueWithFilter =
      new JValueWithFilter(self, x => p(x) && q(x))
  }
  def midFromJson(jsonString: String): Option[ MidParsedSourceMessage ] = {
    val jsonObject = parse(jsonString)

    val mids = for {
      JObject(header) <- jsonObject \ "Header"
      JField("mid", JString(messageId)) <- header
      JField("agid", JString(agentId)) <- header
    } yield (MidParsedSourceMessage( agentId, messageId, jsonString) )

    mids.headOption
  }
  def requestMapFromJsonString( jsonString : String ) : Map[ String, List[ String ] ] = {
    implicit val formats = DefaultFormats
    val jsonObject = parse( jsonString )
    jsonObject.extract[ Map[ String, List[ String ] ] ]
  }
  def writeCommonMessageList( commonMessageList : CommonMessageList ) : String = {
    implicit val formats = org.json4s.DefaultFormats + new CommonMessageDeserializer
    write( commonMessageList.messageList)
  }

}
