/*
 * Copyright (C) 2024 Lectra
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.lectra.kapoeira.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.lectra.kapoeira.Config._
import com.lectra.kapoeira.domain._
import com.typesafe.scalalogging.LazyLogging
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils
import io.confluent.kafka.schemaregistry.json.JsonSchemaUtils
import io.confluent.kafka.serializers.KafkaJsonDeserializerConfig
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, KafkaConsumer}

import java.time.Duration
import java.util.Properties
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object KapoeiraConsumer extends LazyLogging {

  implicit class JsonOutputConfig(outputConfig: OutputConfig) {
    def toJson[A](a: A): String = outputConfig match {
      case _: OutputConfigStringString => a.toString
      case _: OutputConfigStringAvro   => a.toString
      case _: OutputConfigAvroString   => new String(AvroSchemaUtils.toJson(a))
      case _: OutputConfigAvroAvro     => new String(AvroSchemaUtils.toJson(a))
      case _: OutputConfigStringJson   => a.toString
      case _: OutputConfigJsonString   => new String(JsonSchemaUtils.toJson(a))
      case _: OutputConfigJsonJson     => new String(JsonSchemaUtils.toJson(a))
    }
  }

  def createConsumer[K: DataType, V: DataType]: KafkaConsumer[K, V] = {
    val kafkaParams = new Properties()

    kafkaConsumerProperties().foreach { case (key, value) =>
      kafkaParams.put(key, value)
    }
    // specific options
    kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, implicitly[DataType[K]].classDeserializer)
    kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, implicitly[DataType[V]].classDeserializer)
    kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    kafkaParams.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true: java.lang.Boolean)
    kafkaParams.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")
    kafkaParams.put(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, classOf[JsonNode].getName)
    logger.info(s"KAFKA_BOOTSTRAP_SERVERS=${kafkaParams.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)}")
    logger.info(s"""Create consumer with : \n
                   |group.id = ${kafkaParams.get(ConsumerConfig.GROUP_ID_CONFIG)} \n
                   |""".stripMargin)
    logger.debug(kafkaParams.toString)
    new KafkaConsumer[K, V](kafkaParams)
  }

  def consume(
      outputConfig: OutputConfig,
      expectedKeys: Map[String, Int]
  ): Map[String, Seq[ConsumerRecord[String, Any]]] = {
    val consumer = outputConfig.consumer
    val topic = outputConfig.outputConfig.topicName
    val waitDuration = outputConfig.outputConfig.consumerTimeout * 1000
    logger.info(s"Consuming $topic during $waitDuration ms...")

    consumer.assignment().forEach(p => logger.debug(s"BEFORE CONSUME - partition=$p, position=${consumer.position(p)}"))
    consumer
      .endOffsets(consumer.assignment())
      .forEach((k, v) => logger.debug(s"END OFFSETS BEFORE CONSUME - partition=${k.partition()}, position=$v"))

    var kafkaRecords = mutable.Seq[ConsumerRecord[String, Any]]()
    val timer = System.currentTimeMillis()
    val expectedSeq = expectedKeys.toSeq
    while (
      kafkaRecords
        .groupBy(k => k.key())
        .map { case (k, records) => (k, records.size) }
        .toSeq
        .intersect(expectedSeq) != expectedSeq &&
      (timer + waitDuration) > System.currentTimeMillis()
    ) {
      kafkaRecords ++= consumer
        .poll(Duration.ofMillis(waitDuration))
        .records(topic)
        .asScala
        .toSeq
        .map(record =>
          new ConsumerRecord(
            record.topic(),
            record.partition(),
            record.offset(),
            record.timestamp(),
            record.timestampType(),
            record.serializedKeySize(),
            record.serializedValueSize(),
            outputConfig.toJson(record.key()),
            record.value(),
            record.headers(),
            record.leaderEpoch()
          )
        )
    }

    consumer.assignment().forEach(p => logger.debug(s"AFTER CONSUME - partition=$p, position=${consumer.position(p)}"))
    consumer
      .endOffsets(consumer.assignment())
      .forEach((k, v) => logger.debug(s"END OFFSETS AFTER CONSUME - partition=${k.partition()}, position=$v"))
    logger.debug(s"""AFTER CONSUME - Raw records retrieved : \n
                    |${kafkaRecords
      .map(r => s"(${r.partition()},${r.offset()},${r.key()}:${r.value()})")
      .mkString("[", ",", "]")}""".stripMargin)

    kafkaRecords.toSeq.groupBy(r => r.key())
  }
}
