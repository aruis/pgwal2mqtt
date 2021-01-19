package com.aruistar.pgwal2mqtt

import groovy.util.logging.Slf4j
import io.netty.handler.codec.mqtt.MqttQoS
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.mqtt.MqttClient

@Slf4j
class MqttVerticle extends AbstractVerticle {
    @Override
    void start() throws Exception {
        log.info("verticle is starting")

        MqttClient client = MqttClient.create(vertx)

        EventBus eb = vertx.eventBus()

        eb.consumer("db.change", {
            println(it.body().toString())
        })


        client.connect(1883, "mqtt.eclipse.org", s -> {
            client.publish("temperature",
                    Buffer.buffer("hello"),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false);
        });

    }
}
