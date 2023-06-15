/*
 * Copyright (C) 2023 Lectra
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

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.lectra.kapoeira.Config
import com.lectra.kapoeira.Config._
import com.lectra.kapoeira.domain.SubjectFormat.{Avro, Json}
import com.lectra.kapoeira.domain._
import com.lectra.kapoeira.glue.RecordReadOps
import com.typesafe.scalalogging.LazyLogging
import io.confluent.kafka.schemaregistry.avro.{AvroSchema, AvroSchemaUtils}
import io.confluent.kafka.schemaregistry.json.JsonSchemaUtils
import kafka.tools.ConsoleProducer
import kafka.tools.ConsoleProducer.producerProps
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import zio.{Task, ZIO, ZManaged}

import scala.util.{Failure, Try}

object KapoeiraProducer extends LazyLogging {

  private def serializeJson(subject: SubjectConfig, bytes: Array[Byte]): JsonNode = {
    val schemaString =
      requests
        .get(
          s"$KAFKA_SCHEMA_REGISTRY_URL/subjects/${subject.name}/versions/latest/schema"
        )
        .text()
    val value = new String(bytes)
    val mapper = new ObjectMapper()
    val schemaJson = mapper.readTree(schemaString)
    val valueJson: JsonNode = mapper.readTree(value)
    JsonSchemaUtils.envelope(schemaJson, valueJson)
  }

  private def serializeAvro(subject: SubjectConfig, bytes: Array[Byte]): GenericData.Record = {

    val schemaVersions =
      requests
        .get(
          s"$KAFKA_SCHEMA_REGISTRY_URL/subjects/${subject.name}/versions"
        )
        .text()
    val versions: Array[String] = schemaVersions.replace("[", "").replace("]", "").split(",")

    val init: Try[GenericData.Record] = Failure[GenericData.Record](new Exception(s"No schema version found for subject ${subject.name}"))

    versions.foldRight(init) { (version, acc) =>
      if (acc.isFailure) {
        val schemaString =
        requests
          .get(
        s"$KAFKA_SCHEMA_REGISTRY_URL/subjects/${subject.name}/versions/$version/schema"
        )
          .text()
        val parser = new Schema.Parser()
        val schema = parser.parse(schemaString)
        Try(AvroSchemaUtils
          .toObject(new String(bytes), new AvroSchema(schema))
          .asInstanceOf[GenericData.Record])
      }
      else {
        acc
      }
    }.get

  }

  private def producer[K: DataType, V: DataType](topicConfig: TopicConfig): ZManaged[
    Any,
    Throwable,
    KafkaProducer[Any, Any]
  ] = {
    ZManaged
      .make(ZIO.effect {
        val params = commonConfig(topicConfig) ::: (if (JAAS_AUTHENT) jaasConfig else Nil)
        import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
        val props = producerProps(new ConsoleProducer.ProducerConfig(params.toArray))
        props.put(
          ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
          Config.KAFKA_BROKER_LIST
        )
        props.put(
          ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
          implicitly[DataType[K]].classSerializer
        )
        props.put(
          ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
          implicitly[DataType[V]].classSerializer
        )
        props.put("schema.registry.url", Config.KAFKA_SCHEMA_REGISTRY_URL)
        new KafkaProducer[Any, Any](props)
      }) { producer =>
        ZIO
          .effect {
            producer.flush()
            producer.close
          }
          .catchAll(err => ZIO.effectTotal(err.printStackTrace()))
      }
  }

  private def produce[K, V](
                             producer: KafkaProducer[K, V],
                             topic: String,
                             key: K,
                             headers: Map[String, Array[Byte]],
                             recordValue: V
                           ): Task[Unit] = ZIO.effectAsync[Any, Throwable, Unit] { case callback =>
    val record = new ProducerRecord[K, V](topic, key, recordValue)
    headers.foreach { case (k, v) =>
      record.headers().add(k, v)
    }
    producer.send(
      record,
      new Callback {
        override def onCompletion(
                                   metadata: RecordMetadata,
                                   exception: Exception
                                 ): Unit = callback(ZIO.unit)
      }
    )
  }

  def run(
           record: RecordRead,
           topicConfig: TopicConfig,
           keySubjectConfig: Option[SubjectConfig],
           valueSubjectConfig: Option[SubjectConfig]
         ): Task[Unit] = {
    for {
      headers <- ZIO.fromTry(record.jsonHeaders)
      _ <- ((keySubjectConfig, valueSubjectConfig) match {
        case (Some(keySubConf), Some(valueSubConf)) =>
          (keySubConf.format, valueSubConf.format) match {
            case (Avro, Avro) => producer[GenericData.Record, GenericData.Record] _
            case (Avro, Json) => producer[GenericData.Record, JsonNode] _
            case (Json, Avro) => producer[JsonNode, GenericData.Record] _
            case (Json, Json) => producer[JsonNode, JsonNode] _
          }
        case (None, Some(valueSubConf)) =>
          valueSubConf.format match {
            case Avro => producer[String, GenericData.Record] _
            case Json => producer[String, JsonNode] _
          }
        case (Some(keySubConf), None) =>
          keySubConf.format match {
            case Avro => producer[GenericData.Record, String] _
            case Json => producer[JsonNode, String] _
          }
        case _ => producer[String, String] _
      })(topicConfig).use { producer =>
        val keyParsed = keySubjectConfig
          .map(subject =>
            subject.format match {
              case SubjectFormat.Avro => serializeAvro(subject, record.key.getBytes())
              case SubjectFormat.Json => serializeJson(subject, record.key.getBytes())
            }
          )
          .getOrElse(record.key)
        val valueParsed = valueSubjectConfig
          .map(subject =>
            subject.format match {
              case SubjectFormat.Avro => serializeAvro(subject, record.value)
              case SubjectFormat.Json => serializeJson(subject, record.value)
            }
          )
          .getOrElse(new String(record.value))
        produce(producer, topicConfig.topicName, keyParsed, headers, valueParsed)
      }
    } yield ()
  }

  private def commonConfig(
                            topicConfig: TopicConfig
                          ): List[String] =
    List(
      "--topic",
      s"${topicConfig.topicName}",
      "--broker-list",
      s"$KAFKA_BROKER_LIST",
      "--producer-property",
      "max.in.flight.requests.per.connection=1",
      "--producer-property",
      "acks=1",
      "--producer-property",
      "retries=0",
      "--property",
      "parse.key=true"
    )

  private def jaasConfig(): List[String] =
    List(
      "--producer-property",
      "sasl.mechanism=SCRAM-SHA-512",
      "--producer-property",
      "security.protocol=SASL_SSL",
      "--property",
      "security.protocol=SASL_SSL", // FIXME duplicate info?
      "--property",
      "sasl.mechanism=SCRAM-SHA-512",
      "--producer-property",
      s"""sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="$KAFKA_USER" password="$KAFKA_PASSWORD";"""
    )

  object CustomCallback extends Callback {
    override def onCompletion(
                               metadata: RecordMetadata,
                               exception: Exception
                             ): Unit =
      if (exception == null) {
        logger.debug(
          s"PRODUCER (async) - partition=${metadata.partition()} - offset=${metadata.offset()}"
        )
      } else {
        logger.error(exception.getMessage)
      }
  }

}
