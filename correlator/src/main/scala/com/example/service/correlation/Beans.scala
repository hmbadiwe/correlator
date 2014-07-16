package com.example.service.correlation

import scala.beans.BeanProperty

case class Incident(@BeanProperty deviceId: Int, @BeanProperty timeStamp: Long)