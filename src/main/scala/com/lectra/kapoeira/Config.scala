package com.lectra.kapoeira

import java.util.UUID
import com.typesafe.config.ConfigFactory

import java.net.InetAddress
import scala.util.Try

object Config {
  private val config = ConfigFactory.load()
  def uuidTest: String = UUID.randomUUID().toString
  val KAFKA_BROKER_LIST: String = config.getString("kafka.bootstrap.server")
  val KAFKA_USER: String = config.getString("kafka.user")
  val KAFKA_PASSWORD: String = config.getString("kafka.password")
  val hostname = Try{InetAddress.getLocalHost.getHostName}.getOrElse("Unknown")
  def CONSUMER_GROUP: String = s"${config.getString("consumer.group")}-$hostname-$uuidTest"
  val JAAS_AUTHENT: Boolean = config.getBoolean("kafka.authent.isjaas")
  val KAFKA_SCHEMA_REGISTRY_URL = config.getString("kafka.schema.registry.url")
}
