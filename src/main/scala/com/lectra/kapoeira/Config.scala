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
