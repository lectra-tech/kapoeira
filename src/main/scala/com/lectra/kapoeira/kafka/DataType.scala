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
import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}
import io.confluent.kafka.serializers.json.{KafkaJsonSchemaDeserializer, KafkaJsonSchemaSerializer}
import org.apache.avro.generic.GenericData
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

sealed trait DataType[A] {
  type DeserializerT
  type SerializerT
  val classDeserializer: Class[DeserializerT]
  val classSerializer: Class[SerializerT]
}
trait AvroType[T] extends DataType[T] {
  type DeserializerT = KafkaAvroDeserializer
  type SerializerT = KafkaAvroSerializer
  val classDeserializer: Class[KafkaAvroDeserializer] = classOf[KafkaAvroDeserializer]
  val classSerializer: Class[KafkaAvroSerializer] = classOf[KafkaAvroSerializer]
}
case object StringType extends DataType[String] {
  type DeserializerT = StringDeserializer
  type SerializerT = StringSerializer
  val classDeserializer: Class[StringDeserializer] = classOf[StringDeserializer]
  val classSerializer: Class[StringSerializer] = classOf[StringSerializer]
}
case object JsonType extends DataType[JsonNode] {
  type DeserializerT = KafkaJsonSchemaDeserializer[JsonNode]
  type SerializerT = KafkaJsonSchemaSerializer[JsonNode]
  val classDeserializer: Class[KafkaJsonSchemaDeserializer[JsonNode]] = classOf[KafkaJsonSchemaDeserializer[JsonNode]]
  val classSerializer: Class[KafkaJsonSchemaSerializer[JsonNode]] = classOf[KafkaJsonSchemaSerializer[JsonNode]]
}
object DataType {
  implicit val avroType: DataType[Any] = new AvroType[Any] {}
  implicit val avroTypeGeneric: DataType[GenericData.Record] = new AvroType[GenericData.Record] {}
  implicit val stringType: DataType[String] = StringType
  implicit val jsonType: DataType[JsonNode] = JsonType
}
