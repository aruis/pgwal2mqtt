import io.vertx.core.Vertx
import io.vertx.mqtt.MqttClient

vertx = Vertx.vertx()
MqttClient client = MqttClient.create(vertx);

client.connect(31883, "192.168.0.88", ar -> {
    println(ar.succeeded())

    client.publishHandler({ s ->
        System.out.println("There are new message in topic: " + s.topicName());
        System.out.println("Content(as string) of the message: " + s.payload().toString());
        System.out.println("QoS: " + s.qosLevel());
    }).subscribe("pangu", 1);

});
