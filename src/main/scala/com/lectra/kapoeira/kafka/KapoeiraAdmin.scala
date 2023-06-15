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
