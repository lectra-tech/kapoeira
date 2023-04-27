package com.lectra.kapoeira.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.lectra.kapoeira.Config
import com.lectra.kapoeira.Config._
import com.lectra.kapoeira.domain._
import com.typesafe.scalalogging.LazyLogging
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils
import io.confluent.kafka.schemaregistry.json.JsonSchemaUtils
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, KafkaConsumer}
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import io.confluent.kafka.serializers.KafkaJsonDeserializerConfig

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
    kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER_LIST)
    kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, implicitly[DataType[K]].classDeserializer)
    kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, implicitly[DataType[V]].classDeserializer)
    kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP)
    kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    kafkaParams.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true: java.lang.Boolean)
    kafkaParams.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")
    kafkaParams.put("schema.registry.url", Config.KAFKA_SCHEMA_REGISTRY_URL)
    kafkaParams.put(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, classOf[JsonNode].getName)

    logger.info(s"KAFKA_BROKER_LIST=$KAFKA_BROKER_LIST")
    logger.info(s"JAAS_AUTHENT=$JAAS_AUTHENT")
    if (JAAS_AUTHENT) {
      kafkaParams.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512")
      kafkaParams.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name)
      kafkaParams.put(
        SaslConfigs.SASL_JAAS_CONFIG,
        s"org.apache.kafka.common.security.scram.ScramLoginModule required username='$KAFKA_USER' password='$KAFKA_PASSWORD';"
      )
    }
    logger.info(s"""Create consumer with : \n
                   |group.id = $CONSUMER_GROUP \n
                   |""".stripMargin)
    new KafkaConsumer[K, V](kafkaParams)
  }

  def consume(
      outputConfig: OutputConfig,
      expectedKeys: Map[String, Int]
  ): Map[String, Seq[ConsumerRecord[String, Any]]] = {
    val consumer = outputConfig.consumer
    val topic = outputConfig.outputConfig.topicName
    val waitDuration = outputConfig.outputConfig.consumerTimeout * 1000
    logger.info(s"Consuming AVRO $topic during $waitDuration ms...")

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