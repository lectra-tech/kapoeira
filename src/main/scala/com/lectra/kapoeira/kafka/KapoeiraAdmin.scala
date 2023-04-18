package com.lectra.kapoeira.kafka

import com.lectra.kapoeira.Config._
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol

import java.util
import java.util.Properties

object KapoeiraAdmin {
  def createClient = {
    val kafkaParams = new Properties()
    kafkaParams.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER_LIST)
    if (JAAS_AUTHENT) {
      kafkaParams.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512")
      kafkaParams.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name)
      kafkaParams.put(
        SaslConfigs.SASL_JAAS_CONFIG,
        s"org.apache.kafka.common.security.scram.ScramLoginModule required username='$KAFKA_USER' password='$KAFKA_PASSWORD';"
      )
    }
    AdminClient.create(kafkaParams)
  }

}
