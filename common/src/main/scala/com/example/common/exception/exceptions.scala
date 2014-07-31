package com.example.common.exception

case class PhalanxDiagnosticException(msg: Some[String], t: Throwable) extends Exception(msg.getOrElse("No message provided. Consider thorough code review"))
/** These exceptions case classes take Some as the parameter to avoid dealing with empty or null values
  *  Constructor parameter can be either in the form of Some[String] or String itslef. String is possible
  *  in the place of Some[String] because there is a method exists (hopefully in this package) that implicitly
  *  converts String type to Some[String] type.
  */
case class PhalanxRTException(msg: Some[String]) extends RuntimeException(msg.getOrElse("No message provided. Consider thorough code review"))
case class PhalanxException(msg: Some[String]) extends Exception(msg.getOrElse("No message provided. Consider thorough code review"))
case class MalformedIPException(msg: Some[String]) extends RuntimeException(msg.getOrElse("No message provided. Consider thorough code review")) with Serializable
