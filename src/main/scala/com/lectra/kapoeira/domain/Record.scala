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

sealed trait Record {
  val topicAlias: String
  val separator: String = "#"
}

final case class KeyValueRecord(
    topicAlias: String,
    key: String,
    value: String,
    headers: Map[String, Any] = Map.empty,
    batch: Int = 0
) extends Record

final case class KeyValueWithAliasesRecord(
    topicAlias: String,
    key: String,
    valueAlias: String,
    headersAlias: Option[String] = None,
    batch: Int = 0
) extends Record

final case class FileKeyValueRecord(
    topicAlias: String,
    override val separator: String,
    file: String,
    batch: Int = 0
) extends Record

final case class FileValueRecord(
    topicAlias: String,
    key: String,
    file: String,
    batch: Int = 0
) extends Record

final case class FileFormattedValueRecord(
    topicAlias: String,
    key: String,
    file: String,
    batch: Int = 0
) extends Record
