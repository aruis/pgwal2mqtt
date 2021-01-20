package com.aruistar.pgwal2mqtt

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import io.netty.handler.codec.mqtt.MqttQoS
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonArray
import io.vertx.mqtt.MqttClient

@Slf4j
class MqttVerticle extends AbstractVerticle {

    MqttClient client

    @Override
    void start() throws Exception {
        log.info("verticle is starting")
        log.info(config().toString())
        def config = config()
        def host = config.getString("host")
        def port = config.getInteger("port")

        client = MqttClient.create(vertx)

        EventBus eb = vertx.eventBus()

        client.connect(port, host, s -> {
            eb.consumer("db.change", {
                def unique = it.headers().get("unique")
                List body = (it.body() as JsonArray).toList()
                body.each {
                    def map = [
                            schema: it[0],
                            table : it[1],
                            type  : it[2],
                            num   : it[3]
                    ]
                    publish(unique, JsonOutput.toJson(map))
                }
            })
        });

    }

    def publish(String topic, String context) {
        client.publish(topic,
                Buffer.buffer(context),
                MqttQoS.AT_LEAST_ONCE,
                false,
                false)
    }
}
