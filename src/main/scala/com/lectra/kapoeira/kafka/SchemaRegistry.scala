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

import com.fasterxml.jackson.databind.JsonNode
import com.lectra.kapoeira.Config.{KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE, KAFKA_SCHEMA_REGISTRY_BASIC_KEY, KAFKA_SCHEMA_REGISTRY_BASIC_SECRET, KAFKA_SCHEMA_REGISTRY_URL}
import com.lectra.kapoeira.domain.SubjectFormat.{Avro, Json}
import com.lectra.kapoeira.domain.{SubjectConfig, TopicConfig}
import io.confluent.kafka.schemaregistry.SchemaProvider
import io.confluent.kafka.schemaregistry.avro.{AvroSchema, AvroSchemaProvider, AvroSchemaUtils}
import io.confluent.kafka.schemaregistry.client.{CachedSchemaRegistryClient, SchemaRegistryClientConfig}
import io.confluent.kafka.schemaregistry.json.jackson.Jackson
import io.confluent.kafka.schemaregistry.json.{JsonSchemaProvider, JsonSchemaUtils}
import io.confluent.kafka.serializers.json.{KafkaJsonSchemaSerializer, KafkaJsonSchemaSerializerConfig}
import io.confluent.kafka.serializers.{AbstractKafkaSchemaSerDeConfig, KafkaAvroSerializer}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.kafka.common.serialization.StringSerializer

import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava}
import scala.util.{Failure, Try}

object SchemaRegistry {

  private val KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO = "USER_INFO"

  private val schemaProviderList: java.util.List[SchemaProvider] = java.util.List.of(new AvroSchemaProvider, new JsonSchemaProvider)
  private val schemaRegistryClient = new CachedSchemaRegistryClient(
    KAFKA_SCHEMA_REGISTRY_URL,
    AbstractKafkaSchemaSerDeConfig.MAX_SCHEMAS_PER_SUBJECT_DEFAULT,
    schemaProviderList,
    if (KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE==KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO) {
      Map[String, Any](
        SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE -> KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO,
        SchemaRegistryClientConfig.USER_INFO_CONFIG -> s"$KAFKA_SCHEMA_REGISTRY_BASIC_KEY:$KAFKA_SCHEMA_REGISTRY_BASIC_SECRET"
      ).asJava
    } else Map.empty[String, Any].asJava
  )

  // STRING
  private val kafkaStringSerializerConfig = Map.empty[String, Any].asJava
  private val kafkaStringKeySerializer = new StringSerializer
  kafkaStringKeySerializer.configure(kafkaStringSerializerConfig, true)
  private val kafkaStringValueSerializer = new StringSerializer
  kafkaStringValueSerializer.configure(kafkaStringSerializerConfig, false)

  // AVRO
  private val kafkaAvroSerializerConfig = Map[String, Any](
    AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> KAFKA_SCHEMA_REGISTRY_URL,
    AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS -> "true"
  ).asJava
  private val kafkaAvroKeySerializer = new KafkaAvroSerializer(schemaRegistryClient)
  kafkaAvroKeySerializer.configure(kafkaAvroSerializerConfig, true)
  private val kafkaAvroValueSerializer = new KafkaAvroSerializer(schemaRegistryClient)
  kafkaAvroValueSerializer.configure(kafkaAvroSerializerConfig, false)

  // JSON
  private val kafkaJsonSerializerConfig = Map[String, Any](
    AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> KAFKA_SCHEMA_REGISTRY_URL,
    AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS -> "true",
    KafkaJsonSchemaSerializerConfig.FAIL_INVALID_SCHEMA -> "true"
  ).asJava
  private val kafkaJsonKeySerializer = new KafkaJsonSchemaSerializer[JsonNode](schemaRegistryClient)
  kafkaJsonKeySerializer.configure(kafkaJsonSerializerConfig, true)
  private val kafkaJsonValueSerializer = new KafkaJsonSchemaSerializer[JsonNode](schemaRegistryClient)
  kafkaJsonValueSerializer.configure(kafkaJsonSerializerConfig, false)

  private val objectMapper = Jackson.newObjectMapper();

  private def toJsonObject(subjectName: String, input: String): JsonNode = {
    val schemaString = schemaRegistryClient.getLatestSchemaMetadata(subjectName).getSchema
    val schemaJson = objectMapper.readTree(schemaString)
    val valueJson: JsonNode = objectMapper.readTree(input)
    JsonSchemaUtils.envelope(schemaJson, valueJson)
  }

  private def toAvroObject(subjectName: String, input: String): GenericData.Record = {
    val versions: Seq[Integer] = schemaRegistryClient.getAllVersions(subjectName).asScala.toSeq
    val init: Try[GenericData.Record] = Failure[GenericData.Record](new Exception(s"No schema version found for subject $subjectName"))
    versions.foldRight(init) { (version, acc) =>
      if (acc.isFailure) {
        val schemaString = schemaRegistryClient.getByVersion(subjectName, version, false).getSchema
        val parser = new Schema.Parser()
        val schema = parser.parse(schemaString)
        Try(AvroSchemaUtils
          .toObject(input, new AvroSchema(schema))
          .asInstanceOf[GenericData.Record])
      } else acc
    }.get
  }

  private[kafka] def serialize(subjectConfig: Option[SubjectConfig], topicConfig: TopicConfig, input: String, isKey: Boolean) = (subjectConfig, isKey) match {
    case (Some(SubjectConfig(subjectName, _, Avro)), true) => kafkaAvroKeySerializer.serialize(topicConfig.topicName, toAvroObject(subjectName, input))
    case (Some(SubjectConfig(subjectName, _, Avro)), false) => kafkaAvroValueSerializer.serialize(topicConfig.topicName, toAvroObject(subjectName, input))
    case (Some(SubjectConfig(subjectName, _, Json)), true) => kafkaJsonKeySerializer.serialize(topicConfig.topicName, toJsonObject(subjectName, input))
    case (Some(SubjectConfig(subjectName, _, Json)), false) => kafkaJsonValueSerializer.serialize(topicConfig.topicName, toJsonObject(subjectName, input))
    case (_, true) => kafkaStringKeySerializer.serialize(topicConfig.topicName, input)
    case _ => kafkaStringValueSerializer.serialize(topicConfig.topicName, input)
  }

}
