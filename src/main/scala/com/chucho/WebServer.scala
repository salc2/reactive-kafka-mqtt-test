package com.chucho

import java.util.Random

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}
import spray.json._

import scala.io.StdIn
/**
  * Created by chucho on 12/20/16.
  */
object WebServer extends JsonSupport{

  implicit val system = ActorSystem("webserver-system")
  implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher

  private def consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer,
    new StringDeserializer)
    .withBootstrapServers(KafkaMqttMain.kafkaHost)
    .withGroupId("group"+ new Random().nextInt(50))

  private val producerSettings = ProducerSettings(system, new ByteArraySerializer,
    new StringSerializer)
    .withBootstrapServers(KafkaMqttMain.kafkaHost)


  def main(args:Array[String]):Unit = {


    val route =
      path("app" / Segment){ _ =>
        get {
          getFromResource("index.html")
        }
      } ~
      path("ws" / Segment){ rest =>
        extractUpgradeToWebSocket { upgrade =>
          val sink = createSinkWS(rest)
          val source = createSourceWS(rest)
          complete(upgrade.handleMessagesWithSinkSource(sink,source))
        }
      }


    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  def createSourceWS(device:String):Source[Message,_] =
    Consumer.committableSource(consumerSettings,
    Subscriptions.topics("devices.stream"))
    .map(_.record)
    .map(_.value.parseJson)
    .map(_.convertTo[KafkaMessage])
    .filter( kfkMsg => kfkMsg.device.endsWith(device) )
    .map( kfkMsg => TextMessage(kfkMsg.body))

  def createSinkWS(device:String):Sink[Message,_] =
    Sink.foreach[Message]( m =>
      m.asInstanceOf[TextMessage].textStream
          .map{ msg =>
            new ProducerRecord[Array[Byte], String]("devices.commands",
              KafkaMessage(s"/devices/$device",msg).toJson.toString )
          }
      .runWith(Producer.plainSink(producerSettings))
    )

}