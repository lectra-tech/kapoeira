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

import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerConfig

import java.net.InetAddress
import java.util.UUID
import scala.jdk.CollectionConverters._
import scala.util.Try

object Config {
  private val rootConfig = ConfigFactory.load()
  private val hostname = Try{InetAddress.getLocalHost.getHostName}.getOrElse("Unknown")
  private def uuidTest(): String = UUID.randomUUID().toString

  // Kapoeira config
  private val kapoeiraConsumerGroupIdUniqueSuffix = rootConfig.getBoolean("kapoeira.consumer.group.id-unique-suffix")

  // Kafka common config
  val KAFKA_SCHEMA_REGISTRY_URL = rootConfig.getString("kafka.schema.registry.url")
  val KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE = rootConfig.getString("kafka.basic.auth.credentials.source")
  private val userInfo = rootConfig.getString("kafka.basic.auth.user.info").split(":").toSeq
  val KAFKA_SCHEMA_REGISTRY_BASIC_KEY = userInfo.headOption.getOrElse("")
  val KAFKA_SCHEMA_REGISTRY_BASIC_SECRET = userInfo.lastOption.getOrElse("")

  val kafkaCommonProperties = rootConfig.getConfig("kafka").withoutPath("consumer").withoutPath("producer").entrySet().asScala.map(e => e.getKey -> e.getValue.unwrapped()).toMap

  // Kafka consumer config
  private val kafkaConsumerGroupId = rootConfig.getString("kafka.consumer.group.id")
  private val internalKafkaConsumerProperties = kafkaCommonProperties ++ rootConfig.getConfig("kafka.consumer").entrySet().asScala.map(e => e.getKey -> e.getValue.unwrapped()).toMap
  def kafkaConsumerProperties() =
    if (kapoeiraConsumerGroupIdUniqueSuffix) {
      internalKafkaConsumerProperties.filterNot(_._1==ConsumerConfig.GROUP_ID_CONFIG) ++ Map(ConsumerConfig.GROUP_ID_CONFIG -> s"$kafkaConsumerGroupId-$hostname-${uuidTest()}")
    } else internalKafkaConsumerProperties

  // Kafka producer config
  val kafkaProducerProperties = kafkaCommonProperties.concat(rootConfig.getConfig("kafka.producer").entrySet().asScala.map(e => e.getKey -> e.getValue.unwrapped()).toMap)

}
