package com.chucho

import akka.Done
import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSourceSettings}
import akka.stream.scaladsl.Source
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future

/**
  * Created by chucho on 12/20/16.
  */

object SourceMqtt {


  def apply(broker: String, idClient: String, topic: String): Source[MqttMessage, Future[Done]] = {
    val settings = MqttSourceSettings(
      MqttConnectionSettings(
        broker,
        idClient,
        new MemoryPersistence
      ),
      Map(topic -> MqttQoS.AtMostOnce)
    )
    MqttSource(settings, bufferSize = 8)
  }


}



