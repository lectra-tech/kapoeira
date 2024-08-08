package com.lectra.kapoeira.kafka

import com.lectra.kapoeira.Config.{KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE, KAFKA_SCHEMA_REGISTRY_BASIC_KEY, KAFKA_SCHEMA_REGISTRY_BASIC_SECRET, KAFKA_SCHEMA_REGISTRY_URL}
import io.confluent.kafka.schemaregistry.SchemaProvider
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider
import io.confluent.kafka.schemaregistry.client.{CachedSchemaRegistryClient, SchemaRegistryClientConfig}
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.json.{KafkaJsonSchemaSerializer, KafkaJsonSchemaSerializerConfig}
import requests.RequestAuth

import scala.jdk.CollectionConverters.MapHasAsJava

object SchemaRegistry {

  private[kafka] val KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO = "USER_INFO"

  @deprecated private[kafka] val requestAuth : RequestAuth = {
    if (KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE==KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO) {
      (KAFKA_SCHEMA_REGISTRY_BASIC_KEY, KAFKA_SCHEMA_REGISTRY_BASIC_SECRET)
    } else RequestAuth.Empty
  }

  private val schemaProviderList: java.util.List[SchemaProvider] = java.util.List.of(new AvroSchemaProvider, new JsonSchemaProvider)
  private[kafka] val schemaRegistryClient = new CachedSchemaRegistryClient(
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
  private[kafka] val kafkaJsonSerializer = new KafkaJsonSchemaSerializer(
    schemaRegistryClient,
    Map[String, Any](
      AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> KAFKA_SCHEMA_REGISTRY_URL,
      AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS -> "true",
      KafkaJsonSchemaSerializerConfig.FAIL_INVALID_SCHEMA -> "true"
    ).asJava
  )

}
