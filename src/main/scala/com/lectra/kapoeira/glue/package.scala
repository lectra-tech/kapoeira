/*
 * Copyright (C) 2025 Lectra
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
package com.lectra.kapoeira

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.lectra.kapoeira.domain.Services._
import com.lectra.kapoeira.domain.SubjectFormat.{Avro, Json}
import com.lectra.kapoeira.domain._
import com.lectra.kapoeira.kafka.KapoeiraConsumer._
import com.lectra.kapoeira.kafka.{KapoeiraAdmin, KapoeiraConsumer}
import com.typesafe.scalalogging.LazyLogging
import io.confluent.kafka.schemaregistry.json.jackson.Jackson
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition

import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.Try

package object glue extends LazyLogging with RecordReadImplicits {

  implicit val createConsumer: OutputConfigFactory =
    (outputTopicConfig: OutputTopicConfig, subjectConfigs: Map[String, SubjectConfig]) => {
      // String or Avro Consumer?
      val valueFormat = subjectConfigs.get(outputTopicConfig.valueType).map(_.format)
      val keyFormat = subjectConfigs.get(outputTopicConfig.keyType).map(_.format)
      val config: OutputConfig = (keyFormat, valueFormat) match {
        case (Some(Avro), Some(Avro)) =>
          OutputConfigAvroAvro(outputTopicConfig, KapoeiraConsumer.createConsumer[Any, Any])
        case (None, Some(Avro)) =>
          OutputConfigStringAvro(outputTopicConfig, KapoeiraConsumer.createConsumer[String, Any])
        case (Some(Avro), None) =>
          OutputConfigAvroString(outputTopicConfig, KapoeiraConsumer.createConsumer[Any, String])
        case (Some(Json), Some(Json)) =>
          OutputConfigJsonJson(outputTopicConfig, KapoeiraConsumer.createConsumer[JsonNode, JsonNode])
        case (None, Some(Json)) =>
          OutputConfigStringJson(outputTopicConfig, KapoeiraConsumer.createConsumer[String, JsonNode])
        case (Some(Json), None) =>
          OutputConfigJsonString(outputTopicConfig, KapoeiraConsumer.createConsumer[JsonNode, String])
        case (None, None) =>
          OutputConfigStringString(outputTopicConfig, KapoeiraConsumer.createConsumer[String, String])
      }

      val consumer = config.consumer
      logger.debug(
        s"ASSIGNMENT - Consumer assignment on ${outputTopicConfig.topicName}"
      )
      val partitionInfo =
        consumer.partitionsFor(outputTopicConfig.topicName).asScala
      consumer.assign(
        partitionInfo
          .map(p => new TopicPartition(p.topic(), p.partition()))
          .asJava
      )
      logger.debug(
        s"ASSIGNMENT - Consumer assignment ${!consumer.assignment().isEmpty}"
      )
      val consumerPosition = consumer
        .assignment()
        .asScala
        .map(tp => s"topic=$tp, partition=${tp.partition()}, position=${consumer.position(tp)}")
        .mkString("\n")
      logger.debug(consumerPosition)
      consumer.commitSync()

      config
    }

  implicit val kafkaConsume: RecordConsumer = new RecordConsumer {
    override def apply(
        outputConfig: OutputConfig,
        expectedKeys: Map[String, Int]
    ): Map[String, Seq[ConsumerRecord[String, Any]]] =
      consume(outputConfig, expectedKeys)
  }

  implicit val closeConsumer: CloseConsumer =
    (outputConfig: OutputConfig, adminClient: AdminClient) => {
      outputConfig.consumer.unsubscribe()
      outputConfig.consumer.close()
      adminClient.deleteConsumerGroups(java.util.Arrays.asList(outputConfig.consumer.groupMetadata().groupId()))
    }

  implicit val adminClient: AdminClient = KapoeiraAdmin.createClient()

  implicit val openFile: FileOpener = (filePath: String) => {
    logger.info(s"openFile($filePath)")
    val source = Option(
      Thread.currentThread.getContextClassLoader.getResource(filePath)
    ) match {
      case None        => Source.fromFile(s"/$filePath")
      case Some(value) => Source.fromFile(value.getPath)
    }
    val result = source.getLines().toList
    source.close()
    result
  }

  object ConsoleTimer {
    def time[R](label: String, block: => R): R = {
      logger.info(s"$label...")
      val t0 = System.currentTimeMillis()
      val result = block // call-by-name
      val duration = (System.currentTimeMillis() - t0).toDouble / 1000d
      logger.info(s"$label... DONE, elapsed time=%.3fs".format(duration))
      result
    }
  }

  private[glue] val objectMapper = Jackson.newObjectMapper();

  implicit class RecordReadOps(recordRead: RecordRead) {
    def jsonHeaders: Try[Map[String, Array[Byte]]] = Try(
      recordRead.headers.map {
        case (k, v: String) => (k, v.getBytes)
        case (k, v)         => (k, objectMapper.writeValueAsBytes(v))
      }
    )
  }

  implicit val readHeadersString: ReadHeaders[String] = (string: String) =>
    Try {
      recursiveConversion(
        objectMapper
          .readValue(string, classOf[java.util.HashMap[String, Object]])
      )
    }.getOrElse(Map.empty)

  implicit class ReadHeadersOps[T: ReadHeaders](val headers: T) {
    def readHeaders: Map[String, Any] = implicitly[ReadHeaders[T]].readHeaders(headers)
  }

  private def recursiveConversion(
      map: java.util.Map[String, Object]
  ): Map[String, Any] = {
    map.asScala.toMap.map {
      case (k, v: java.util.Map[String, Object]) => (k, recursiveConversion(v))
      case (k, v)                                => (k, v)
    }
  }
}
