# reactive-kafka-mqtt-test #

## Context ##
Playing with Kafka, Akka and MQTT to emulate ioT devices

This was just a proof of concept bulding "from scratch" a iot communication. 
Using Akka Stream/Http, kafka, Alpakka, Mqtt, eclipse paho and ofcourse Scala.

```$xslt
 +--------+  +--------+  +---------+
 |  device01 |device02|  |device03 |
 +--+--^--+  +-----^--+  +------^--+
    |  |        |  |        |   |
+------+--------v--+--------v---+---+
|   v                               <----------+
|        MQTT - Broker              +-------+  |
+-------------+---------------------+       |  |
                                            |  |
+---------------+     +---------------------v--+---------------------+
|               +----->                                              |
|               |     |              Kafka                           |
| web server    <------+                                             |
| Akka-http     |     +----------------------------------------------+
+---------------+
```

## What do you need to run it ##
   - sbt
   - docker
   
## How to prepare the environment ##
```shell
$ docker pull spotify/kafka
$ docker pull emqttd/emqttd
$ docker run -d --name kafka -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=172.17.0.1 --env ADVERTISED_PORT=9092 spotify/kafka
$ docker run -d --name emq -p 18083:18083 -p 1883:1883 emqttd/emqttd:latest /opt/emqttd/bin/emqttd_start
```
> NOTE: Here _ADVERTISED_HOST=172.17.0.1_ you would need to replace it with your docker ip host usually it is 172.17.0.1

## Run the application and play with it ##

Open a terminal a run:
```shell
$ sbt "runMain com.chucho.KafkaMqttMain"
```
Open a second terminal a run:
```shell
$ sbt "runMain com.chucho.WebServer"
```
Open a third more terminal a run:
```shell
$ sbt "runMain com.chucho.DeviceEmulatorMain device01"
```

Go to the browser to `localhost:8080/app/device01` and you can watch data collected by that device (just the `free` unix command each 500ms). 
You could also send it messages, it will print messages out in the terminal where it runs.
 
You can run as many device emulator as you want. For any N number of devices
(`$ sbt "runMain com.chucho.DeviceEmulatorMain <device_name>"`) 
You can watch the data collected by them going to `localhost:8080/app/<device_name>` 





