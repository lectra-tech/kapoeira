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
package com.lectra.kapoeira.kafka

import com.lectra.kapoeira.Config._
import com.lectra.kapoeira.domain._
import com.lectra.kapoeira.glue.RecordReadOps
import com.lectra.kapoeira.kafka.SchemaRegistry._
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization.ByteArraySerializer
import zio.{Task, ZIO}

import java.util.Properties

object KapoeiraProducer extends LazyLogging {

  private val producerProperties = {
    val kafkaParams = new Properties()
    kafkaProducerProperties.foreach { case (key, value) =>
      kafkaParams.put(key, value)
    }
    // specific options
    kafkaParams.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[ByteArraySerializer].getCanonicalName)
    kafkaParams.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[ByteArraySerializer].getCanonicalName)
    kafkaParams.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    kafkaParams.put(ProducerConfig.ACKS_CONFIG, "1")
    kafkaParams.put(ProducerConfig.RETRIES_CONFIG, "0")
    kafkaParams
  }

  private val producerZIO = ZIO.acquireRelease(ZIO.attempt {
      new KafkaProducer[Array[Byte], Array[Byte]](producerProperties)
    }) { producer =>
      ZIO
        .attempt {
          producer.flush()
          producer.close()
        }
        .catchAll(err => ZIO.succeed(err.printStackTrace()))
    }

  private def produce(
                       producer: KafkaProducer[Array[Byte], Array[Byte]],
                       topic: String,
                       headers: Map[String, Array[Byte]],
                       key: Array[Byte],
                       recordValue: Array[Byte]
                     ): Task[Unit] = ZIO.async[Any, Throwable, Unit] { callback =>
    val record = new ProducerRecord[Array[Byte], Array[Byte]](topic, key, recordValue)
    headers.foreach { case (k, v) =>
      record.headers().add(k, v)
    }
    producer.send(record, (_: RecordMetadata, _: Exception) => callback(ZIO.unit))
  }

  def run(
           record: RecordRead,
           topicConfig: TopicConfig,
           keySubjectConfig: Option[SubjectConfig],
           valueSubjectConfig: Option[SubjectConfig]
         ): Task[Unit] = {
    for {
      headers <- ZIO.fromTry(record.jsonHeaders)
      key = serialize(keySubjectConfig, topicConfig, record.key, isKey = true)
      value = serialize(valueSubjectConfig, topicConfig, record.value, isKey = false)
      _ <- ZIO.scoped(producerZIO.flatMap(produce(_, topicConfig.topicName, headers, key, value)))
    } yield ()
  }
}
