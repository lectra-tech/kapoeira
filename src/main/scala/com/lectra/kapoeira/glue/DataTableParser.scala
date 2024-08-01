/*
 * Copyright (C) 2024 Lectra
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
package com.lectra.kapoeira.glue

import com.lectra.kapoeira.domain.{
  FileFormattedValueRecord,
  FileKeyValueRecord,
  FileValueRecord,
  InputTopicConfig,
  KeyValueRecord,
  KeyValueWithAliasesRecord,
  OutputTopicConfig,
  SubjectConfig,
  SubjectFormat
}
import com.typesafe.scalalogging.LazyLogging
import io.cucumber.datatable.DataTable

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object DataTableParser extends LazyLogging {

  def parseInputTopicDataTable(records: DataTable): List[InputTopicConfig] = {
    records
      .asMaps()
      .asScala
      .map(_.asScala)
      .map(row => InputTopicConfig(row("topic"), row("alias"), row("key_type"), row("value_type")))
      .toList
  }

  def parseOutputTopicDataTable(records: DataTable): List[OutputTopicConfig] = {
    records
      .asMaps()
      .asScala
      .map(_.asScala)
      .map(row => {
        Try(row("readTimeoutInSecond").toInt) match {
          case Success(timeout) => OutputTopicConfig(row("topic"), row("alias"), row("key_type"), row("value_type"), timeout)
          case Failure(e) =>
            logger.error(e.getMessage)
            OutputTopicConfig(row("topic"), row("alias"), row("key_type"), row("value_type"))
        }
      })
      .toList
  }

  def parseSubjectDataTable(records: DataTable): List[SubjectConfig] = {
    records
      .asMaps()
      .asScala
      .map(_.asScala)
      .map(row => {
        val rowFormat = row("format")
        val subjectFormat = SubjectFormat
          .parse(rowFormat)
          .fold {
            val message = s"format is not known : $rowFormat, valid formats are ${SubjectFormat.values}"
            logger.error(message)
            throw new IllegalArgumentException(message)
          }(identity)
        SubjectConfig(row("name"), row("alias"), subjectFormat)
      })
      .toList
  }

  def parseKeyValueDataTable(records: DataTable): List[KeyValueRecord] = {
    records
      .asMaps()
      .asScala
      .map(_.asScala)
      .map(row =>
        KeyValueRecord(
          row("topic_alias"),
          row("key"),
          row("value"),
          row.getOrElse("headers", "").readHeaders,
          row.view.mapValues(Integer.parseInt).getOrElse("batch", 0)
        )
      )
      .toList
  }

  def parseKeyValueAliasesDataTable(records: DataTable): List[KeyValueWithAliasesRecord] = {
    records
      .asMaps()
      .asScala
      .map(_.asScala)
      .map(row =>
        KeyValueWithAliasesRecord(
          row("topic_alias"),
          row("key"),
          row("value"),
          row.get("headers"),
          row.view.mapValues(Integer.parseInt).getOrElse("batch", 0)
        )
      )
      .toList
  }

  def parseFileKeyValueDataTable(records: DataTable): List[FileKeyValueRecord] = {
    records
      .asMaps()
      .asScala
      .map(_.asScala)
      .map(row =>
        FileKeyValueRecord(
          row("topic_alias"),
          row("separator"),
          row("file"),
          row.view.mapValues(Integer.parseInt).getOrElse("batch", 0)
        )
      )
      .toList
  }

  def parseFileValueDataTable(records: DataTable): List[FileValueRecord] = {
    records
      .asMaps()
      .asScala
      .map(_.asScala)
      .map(row =>
        FileValueRecord(row("topic_alias"), row("key"), row("file"), row.view.mapValues(Integer.parseInt).getOrElse("batch", 0))
      )
      .toList
  }

  def parseFileFormattedValueDataTable(records: DataTable): List[FileFormattedValueRecord] = {
    records
      .asMaps()
      .asScala
      .map(_.asScala)
      .map(row =>
        FileFormattedValueRecord(
          row("topic_alias"),
          row("key"),
          row("file"),
          row.view.mapValues(Integer.parseInt).getOrElse("batch", 0)
        )
      )
      .toList
  }

}
