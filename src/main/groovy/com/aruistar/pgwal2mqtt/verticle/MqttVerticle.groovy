package com.aruistar.pgwal2mqtt.verticle

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import io.netty.handler.codec.mqtt.MqttQoS
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonArray
import io.vertx.mqtt.MqttClient

@Slf4j
class MqttVerticle extends AbstractVerticle {

    MqttClient client
    EventBus eb
    MessageConsumer messageConsumer

    @Override
    void start() throws Exception {
        log.info("verticle is starting")
        log.info(config().toString())
        def config = config()
        def host = config.getString("host")
        def port = config.getInteger("port")


        eb = vertx.eventBus()

        connect(port, host)


    }

    def connect(int port, String host) {
        log.info("want connect mqtt...")
        if (client?.connected) {
            client.disconnect()
        }

        client = MqttClient.create(vertx)
        client.connect(port, host, s -> {
            if (s.succeeded()) {
                if (messageConsumer) messageConsumer.unregister()

                messageConsumer = eb.consumer("db.change", {
                    def unique = it.headers().get("unique")
                    List body = (it.body() as JsonArray).toList()
                    body.each {
                        def map = [
                                schema: it[0],
                                table : it[1],
                                type  : it[2],
                                num   : it[3]
                        ]
                        publish(client, unique, JsonOutput.toJson(map))
                    }
                })
            } else {
                log.error(s.cause().message)

                vertx.setTimer(2_000, {
                    connect(port, host)
                })
            }
        })

        client.closeHandler({
            connect(port, host)
        })
    }

    static def publish(MqttClient client, String topic, String context) {
        log.info(context)
        log.info(client.connected.toString())
        if (!client.connected) return

        client.publish(topic,
                Buffer.buffer(context),
                MqttQoS.AT_LEAST_ONCE,
                false,
                false)
    }
}
