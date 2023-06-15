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

import com.lectra.kapoeira.domain.Services.{CloseConsumer, OutputConfigFactory, RecordConsumer}
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerRecord

class BackgroundContext extends LazyLogging {

  var inputTopicConfigs: Map[String, InputTopicConfig] = Map.empty
  var outputConfigs: Map[String, OutputConfig] = Map.empty
  var variables: Map[String, String] = Map.empty
  var subjectConfigs: Map[String, SubjectConfig] = Map.empty

  def addInput(inputTopicConfig: InputTopicConfig) =
    this.inputTopicConfigs = this.inputTopicConfigs ++ Map(inputTopicConfig.alias -> inputTopicConfig)

  def addOutput(outputTopicConfig: OutputTopicConfig)(implicit outputConfigFactory: OutputConfigFactory) =
    this.outputConfigs = this.outputConfigs ++ Map(
      outputTopicConfig.alias -> outputConfigFactory(outputTopicConfig, this.subjectConfigs)
    )

  def addSubject(subjectConfig: SubjectConfig) =
    this.subjectConfigs = this.subjectConfigs ++ Map(subjectConfig.alias -> subjectConfig)

  def close()(implicit closeConsumer: CloseConsumer, adminClient: AdminClient) =
    try {
      this.outputConfigs.values.foreach(outputConfig => closeConsumer(outputConfig, adminClient))
    } catch {
      case e: Exception => logger.error(e.getMessage)
    } // FIXME

  def consumeTopic(topicAlias: String, keys: Map[String, Int])(implicit
      recordConsumer: RecordConsumer
  ): Map[String, Seq[ConsumerRecord[String, Any]]] = {
    this.outputConfigs.get(topicAlias) match {
      case Some(outputConfig) => recordConsumer(outputConfig, keys)
      case _                  => throw new IllegalArgumentException(s"missing $topicAlias in background")
    }
  }

  def addVariable(key: String, value: String): Unit =
    this.variables = this.variables.updated(key, value)

  def getVariable(key: String): Option[String] = this.variables.get(key)

  def substituteVariablesIn(string: String): String = string match {
    case null | "" => string
    case _ =>
      variables.keys.foldLeft(string) { case (acc, key) =>
        acc.replaceAll(s"\\$$\\{$key\\}", variables.getOrElse(key, "$${key}"))
      }
  }
}
