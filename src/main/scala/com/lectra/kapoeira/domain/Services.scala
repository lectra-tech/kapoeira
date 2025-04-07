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
package com.lectra.kapoeira.domain

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.{Task, ZIO}

object Services {

  trait OutputConfigFactory {
    def apply(
               outputTopicConfig: OutputTopicConfig,
               subjectConfigs: Map[String, SubjectConfig]
             ): OutputConfig
  }

  trait RecordConsumer {
    def apply(
               outputConfig: OutputConfig,
               expectedKeys: Map[String, Int]
             ): Map[String, Seq[ConsumerRecord[String, Any]]]
  }

  trait RecordProducer {
    def run(
             record: RecordRead,
             topicConfig: TopicConfig,
             keySubjectConfig: Option[SubjectConfig],
             valueSubjectConfig: Option[SubjectConfig]
           ): Task[Unit]
  }

  trait CloseConsumer {
    def apply(outputConfig: OutputConfig, adminClient: AdminClient): Unit
  }

  trait FileOpener {
    def apply(filePath: String): List[String]
  }

  trait ReadHeaders[T] {
    def readHeaders(t: T): Map[String, Any]
  }
}