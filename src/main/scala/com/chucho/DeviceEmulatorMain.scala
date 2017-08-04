package com.chucho
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{IMqttMessageListener, MqttClient, MqttConnectOptions, MqttMessage}

import scala.sys.process._
/**
  * Created by chucho on 12/20/16.
  */
object DeviceEmulatorMain {

  def main(args:Array[String]):Unit = {

    val device = MyMqttClient("tcp://localhost:1883",args(0))
    var delta = System.currentTimeMillis

    device.subscribe(s"/devices/${args(0)}/command", (_: String, message: MqttMessage) => {
      println(s"RECEIVED: ${new String(message.getPayload)}")
    })

    while(true){
      if(System.currentTimeMillis - delta > 500){
        val msg =  Process("free").lineStream.toList.mkString(""";""")
        device.publish(s"/devices/${args(0)}/stream",msg.getBytes)
        delta = System.currentTimeMillis
      }
    }
  }
}


case class MyMqttClient(brokerHost:String, clientId:String) {
  private val persistence = new MemoryPersistence
  private val sampleClient = new MqttClient(brokerHost, clientId, persistence)

  private val optCon = new MqttConnectOptions {
    setCleanSession(true)
  }
  System.out.println(s"$clientId Connecting to broker: $brokerHost")
  sampleClient.connect(optCon)

  def subscribe(topic:String, listener:IMqttMessageListener):Unit ={
    sampleClient.subscribe(topic,listener)
  }

  def publish(topic:String,msg:Array[Byte]):Unit ={
    sampleClient.publish(topic,new MqttMessage(msg))
  }
}
