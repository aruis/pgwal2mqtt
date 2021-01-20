package com.aruistar.pgwal2mqtt

import com.aruistar.pgwal2mqtt.verticle.DatabaseVerticle
import com.aruistar.pgwal2mqtt.verticle.MqttVerticle
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions

@Slf4j
class MainVerticle extends AbstractVerticle {

    @Override
    void start() throws Exception {
        log.info("verticle is starting")
        log.info(config().toString())

        def dbs = config().getJsonArray("dbs").toList()
        dbs.each { it ->
            vertx.deployVerticle(DatabaseVerticle.newInstance(), new DeploymentOptions().setConfig(it))
        }

        vertx.deployVerticle(MqttVerticle.newInstance(), new DeploymentOptions().setConfig(config().getJsonObject("mqtt")))

    }
}
