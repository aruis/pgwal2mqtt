package com.aruistar.pgwal2mqtt

import com.julienviet.childprocess.Process
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonArray

@Slf4j
class DatabaseVerticle extends AbstractVerticle {

    @Override
    void start() throws Exception {
        log.info("verticle is starting")
        log.info(config().toString())
        def config = config()

        def eb = vertx.eventBus()
        def jsonSlurper = new JsonSlurper()

        def dbName = config.getString("database")
        def username = config.getString("username")
        def host = config.getString("host")
        def unique = config.getString("unique")
        def port = config.getInteger("port")

        def slotName = "test_slot"

        "pg_recvlogical -h ${host} -p ${port} -U ${username} -d ${dbName} --slot ${slotName} --create-slot -P wal2json".execute()

        Process.create(vertx, "pg_recvlogical",
                [
                        "-h", host,
                        "-p", port.toString(),
                        "-U", username,
                        "-d", dbName,
                        "--slot", slotName,
                        "--start",
//                        "-o", "pretty-print=true",
                        "-o", "add-msg-prefixes=wal2json",
//                        "-o", "include-timestamp=true",
//                        "-o", "include-type-oids=true",
//                        "-o", "write-in-chunks=true",
                        "-o", "format-version=1",
                        "-f", "-"
                ].toList()).start(process -> {
            process.stdout().handler(buf -> {

                buf.toString().eachLine {
                    try {
                        Map json = jsonSlurper.parseText(it)
                        if (json.containsKey("change")) {
                            def list = json.change
                            def schemas = []
                            list.each {
                                def row = [it.schema, it.table, it.kind, 1]

                                def match = schemas.find {
                                    it[0] == row[0]
                                            && it[1] == row[1]
                                            && it[2] == row[2]
                                }

                                if (match) {
                                    match[3]++
                                } else {
                                    schemas << row
                                }

                            }

                            eb.send("db.change", new JsonArray(schemas), new DeliveryOptions().addHeader("unique", unique))
                        }
                    } catch (e) {
                        log.error("catch error", e)
                    }
                }
            })
        })
    }
}
