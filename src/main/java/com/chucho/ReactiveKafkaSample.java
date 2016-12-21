package com.chucho;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Date;
import java.util.concurrent.CompletionStage;

/**
 * Created by chucho on 12/19/16.
 */
public class ReactiveKafkaSample {

    public static void main(String[] args) {

        final ActorSystem system = ActorSystem.create("reactive-kafka");
        final ActorMaterializer materializer = ActorMaterializer.create(system);


        final ConsumerSettings<byte[], String> consumerSettings =
                ConsumerSettings.create(system, new ByteArrayDeserializer(), new StringDeserializer())
                        .withBootstrapServers("192.168.104.14")
                        .withGroupId("group1")
                        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final ProducerSettings<byte[], String> producerSettings = ProducerSettings
                .create(system, new ByteArraySerializer(), new StringSerializer())
                .withBootstrapServers("192.168.104.14:9092");

        Consumer.plainSource(
                consumerSettings,
                Subscriptions.assignmentWithOffset(new TopicPartition("device", 0), 1)
        ).runWith(Sink.foreach( record -> {
            System.err.println("Device: "+record.topic());
            System.err.println("Message: "+record.value());
            System.err.println("Time: "+new Date(record.timestamp()).toString());
        } ), materializer);

        CompletionStage<Done> done =
                Source.range(1, 100)
                        .map(n -> n.toString()).map(elem -> new ProducerRecord<byte[],
                        String>("device", elem))
                        .runWith(Producer.plainSink(producerSettings), materializer);


    }

}
