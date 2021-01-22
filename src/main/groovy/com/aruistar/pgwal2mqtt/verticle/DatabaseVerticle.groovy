package com.aruistar.pgwal2mqtt.verticle

import com.julienviet.childprocess.Process
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

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
        def slotName = config.getString("unique", "pgwal2mqtt_slot")
        def port = config.getInteger("port")
        def summary = config.getBoolean("summary", false)

        List<String> exclude = config.getJsonArray("exclude", new JsonArray()).toList() as List<String>
        List<String> include = config.getJsonArray("include", new JsonArray()).toList() as List<String>


        def filterTables = exclude.join(",")
        def addTables = include.join(",")

        "pg_recvlogical -h ${host} -p ${port} -U ${username} -d ${dbName} --slot ${slotName} --create-slot -P wal2json".execute()

        def params = [
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
        ]

        if (exclude.size() > 0) {
            params << "-o"
            params << "filter-tables=${filterTables}".toString()
        }

        if (include.size() > 0) {
            params << "-o"
            params << "add-tables=${addTables}".toString()
        }

        Process.create(vertx, "pg_recvlogical", params.toList()).start(process -> {
            process.stdout().handler(buf -> {

                buf.toString().eachLine {
                    try {
                        if (!it.isBlank()) {
                            Map json = jsonSlurper.parseText(it)
                            if (json.containsKey("change")) {
                                List list = json.change
                                if (list.size() > 0) {
                                    if (summary) {
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
                                        schemas.each {
                                            eb.send("db.change", new JsonObject([
                                                    schema: it[0],
                                                    table : it[1],
                                                    type  : it[2],
                                                    num   : it[3]
                                            ]), new DeliveryOptions().addHeader("unique", unique))
                                        }

                                    } else {
                                        eb.send("db.change", new JsonArray(list), new DeliveryOptions().addHeader("unique", unique))
                                    }
                                }

                            }
                        }
                    } catch (e) {
                        log.error("catch error", e)
                    }
                }
            })
        })
    }
}
