package com.example.persistence.lucene

import com.example.common.domain.CommonMessage


object QueryBuilder {
    private val RangeRegex = """\((\-?\d+),(\-?\d+)\)""".r
    private def generateAndQuery( message : CommonMessage, field : String) : String = {

      val fieldAndedArray = field.split("/")
      val fieldAndQueriesOption = fieldAndedArray.map { andedField =>
        val fieldArray = andedField.trim.split("<>")
        if( fieldArray.isEmpty) Some("")
        else if( fieldArray.size == 1 ) {
          message.get( fieldArray(0)).map{ fieldValue =>
            fieldArray(0) + ":\"" + fieldValue + "\""
          }
        }
        else {
          message.get( fieldArray(0) ).map{ messageValue =>
            val range = fieldArray(1).trim
            range match{
              case RangeRegex( negativeX, positiveX ) => {
                val center = messageValue.toLong
                val start = negativeX.toLong + center
                val finish = positiveX.toLong + center
                s"${fieldArray(0)}:[${ start } TO ${ finish }]"
              }
              case _ => ""
            }
          }
          
        }
      }
      val filteredFields = fieldAndQueriesOption.map{ _.getOrElse("")  }.filter(!_.trim.isEmpty)
      if( filteredFields.isEmpty )""
      else filteredFields.mkString("(" ," AND ", ")")
     }

     def generateQuery( message : CommonMessage ) : String = {
       val correlationStringOption = message.get("correlationLevel").map{ correlationLevel =>
         val correlationFields = correlationLevel.trim.split("\\|")
         val andQueryList = correlationFields.map{ field => generateAndQuery( message, field ) }
         andQueryList.filter(! _.trim.isEmpty )mkString( " OR ")
       }
      correlationStringOption.getOrElse("")
     }




}
