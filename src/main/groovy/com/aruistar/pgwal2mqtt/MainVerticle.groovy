package com.aruistar.pgwal2mqtt


import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx

@Slf4j
class MainVerticle extends AbstractVerticle {


    static void main(String[] args) {
        Vertx.vertx().deployVerticle(new MainVerticle())
    }

    @Override
    void start() throws Exception {
        vertx.deployVerticle(MqttVerticle.newInstance())
        vertx.deployVerticle(PgRecvlogical.newInstance())
    }
}
