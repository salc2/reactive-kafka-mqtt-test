package com.chucho

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}
import spray.json._

/**
  * Created by chucho on 12/19/16.
  */

object KafkaMqttMain extends JsonSupport{

  val kafkaHost = "192.168.1.43:9092"
  val mqttHost = "tcp://172.17.0.2:1883"

  def main(arg: Array[String]): Unit = {

    lazy implicit val system = ActorSystem("reactive-kafka")
    lazy implicit val materializer = ActorMaterializer()
    val pubMqttClient = MyMqttClient(mqttHost,"pub_n1")


   val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(kafkaHost)
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    Consumer.committableSource(consumerSettings,
      Subscriptions.topics("devices.commands"))
      .map(_.record)
        .map( re =>{
          println("aqui en el consumer "+re)
          re
        })
      .map(_.value.toJson)
      .map(_.convertTo[KafkaMessage])
      .runWith(Sink.foreach( kfMsg => {
        pubMqttClient.publish(s"${kfMsg.device}/command",kfMsg.body.getBytes)
      } ))(materializer)

    val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers(kafkaHost)

    val sourceMqtt = SourceMqtt(mqttHost,"sub#1","/devices/+/stream")
    sourceMqtt
        .map{ mqttMsg =>
          val topic = mqttMsg.topic.replace("/stream","")
          KafkaMessage(topic, mqttMsg.payload.decodeString("UTF-8"))
        }.map{ msgKafka =>
          new ProducerRecord[Array[Byte], String]("devices.stream", msgKafka.toJson.toString )
        }.runWith( Producer.plainSink(producerSettings) )
  }
}

case class KafkaMessage(device:String, body:String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat:JsonFormat[KafkaMessage] = jsonFormat2(KafkaMessage)
}




