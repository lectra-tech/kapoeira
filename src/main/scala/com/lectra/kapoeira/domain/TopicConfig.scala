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
package com.lectra.kapoeira.domain

import com.fasterxml.jackson.databind.JsonNode
import com.lectra.kapoeira.kafka.DataType
import org.apache.avro.generic.GenericData
import org.apache.kafka.clients.consumer.KafkaConsumer

sealed trait TopicConfig {
  val topicName: String
  val alias: String
  val keyType: String
  val valueType: String
  def keyIsAvro: Boolean = keyType.toLowerCase().trim != "string"
  def valueIsAvro: Boolean = valueType.toLowerCase().trim != "string"
}

final case class InputTopicConfig(topicName: String, alias: String, keyType: String, valueType: String) extends TopicConfig

final case class OutputTopicConfig(topicName: String, alias: String, keyType: String, valueType: String, consumerTimeout: Int = 2)
    extends TopicConfig

sealed trait OutputConfig {
  type KeyType
  type ValueType
  val outputConfig: OutputTopicConfig
  val consumer: KafkaConsumer[KeyType, ValueType]
}
final case class OutputConfigStringString(outputConfig: OutputTopicConfig, consumer: KafkaConsumer[String, String])
    extends OutputConfig {
  type KeyType = String
  type ValueType = String
}
final case class OutputConfigStringAvro(outputConfig: OutputTopicConfig, consumer: KafkaConsumer[String, Any])
    extends OutputConfig {
  type KeyType = String
  type ValueType = Any
}
final case class OutputConfigAvroString(outputConfig: OutputTopicConfig, consumer: KafkaConsumer[Any, String])
    extends OutputConfig {
  type KeyType = Any
  type ValueType = String
}
final case class OutputConfigAvroAvro(outputConfig: OutputTopicConfig, consumer: KafkaConsumer[Any, Any]) extends OutputConfig {
  type KeyType = Any
  type ValueType = Any
}

final case class OutputConfigStringJson(outputConfig: OutputTopicConfig, consumer: KafkaConsumer[String, JsonNode])
    extends OutputConfig {
  type KeyType = String
  type ValueType = JsonNode
}

final case class OutputConfigJsonJson(outputConfig: OutputTopicConfig, consumer: KafkaConsumer[JsonNode, JsonNode])
    extends OutputConfig {
  type KeyType = JsonNode
  type ValueType = JsonNode
}

final case class OutputConfigJsonString(outputConfig: OutputTopicConfig, consumer: KafkaConsumer[JsonNode, String])
    extends OutputConfig {
  type KeyType = JsonNode
  type ValueType = String
}

final case class SubjectConfig(name: String, alias: String, format: SubjectFormat)

sealed trait SubjectFormat

object SubjectFormat {
  case object Avro extends SubjectFormat {
    override def toString() = "avro"
  }
  case object Json extends SubjectFormat {
    override def toString() = "json"
  }
  val values = List(Avro, Json)
  def parse(format: String) =
    format.toLowerCase() match {
      case "avro" => Some(Avro)
      case "json" => Some(Json)
      case _      => None
    }
}
