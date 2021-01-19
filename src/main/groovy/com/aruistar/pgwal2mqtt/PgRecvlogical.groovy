package com.aruistar.pgwal2mqtt

import com.julienviet.childprocess.Process
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle

@Slf4j
class PgRecvlogical extends AbstractVerticle {

    @Override
    void start() throws Exception {
        log.info("verticle is starting")
        def eb = vertx.eventBus()
        def jsonSlurper = new JsonSlurper()
        def dbName = 'postgres'

        def slotName = "test_slot"

        "pg_recvlogical -d ${dbName} --slot ${slotName} --create-slot -P wal2json".execute()

        Process.create(vertx, "/opt/homebrew/bin/pg_recvlogical",
                [
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

                            eb.send("db.change", JsonOutput.toJson(schemas))
                        }
                    } catch (e) {
                        log.error("catch error", e)
                    }
                }
            })
        })
    }
}
