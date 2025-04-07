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

import com.lectra.kapoeira.domain
import com.lectra.kapoeira.domain.Services.ReadHeaders

import scala.util.Try

/** The record read from the cucumber Datatable directly, or a File.
  */
final case class RecordRead(
    topicAlias: String,
    key: String,
    value: String,
    headers: Map[String, Any]
)

/** Interpolate variables of T with the backgroundContext.
  */
trait Interpolate[T] {
  def interpolate(t: T, backgroundContext: BackgroundContext): T
}

/** Map a T to a list of RecordRead. Used to map domain.Record ADT related to a file.
  */
trait RecordDataFromFile[T] {
  def readFromFile(t: T, f: String => List[String]): List[RecordRead]
}

/** Map a T to a list of RecordRead. Used to map domain.Record ADT related to a values in Datatable.
  */
trait RecordData[T] {
  def read(t: T): RecordRead
}

/** Facilitate import of all implicits.
  */
trait RecordReadImplicits
    extends InterpolateImplicits
    with RecordDataImplicits
    with RecordDataFromFileImplicits

trait InterpolateImplicits {

  implicit val interpolateString: Interpolate[String] =
    (t: String, ctx: BackgroundContext) => ctx.substituteVariablesIn(t)
  implicit val interpolateRecordRead: Interpolate[RecordRead] =
    (t: RecordRead, ctx: BackgroundContext) => {
      RecordRead(
        ctx
          .substituteVariablesIn(t.topicAlias),
        ctx
          .substituteVariablesIn(t.key),
        ctx
          .substituteVariablesIn(t.value),
        interpolateMap(t.headers, ctx)
      )
    }

  //runtime inspection of iterable to interpolate ...
  private def interpolateIterable(
      xs: Iterable[Any],
      ctx: BackgroundContext
  ): Iterable[Any] =
    xs.map {
      case s: String           => ctx.substituteVariablesIn(s)
      case m: Map[String, Any] => interpolateMap(m, ctx)
      case x: Iterable[Any]    => interpolateIterable(x, ctx)
      case _                   => xs
    }

  //runtime inspection of Map to interpolate ...
  private def interpolateMap(
      map: Map[String, Any],
      ctx: BackgroundContext
  ): Map[String, Any] = map.map {
    case (k, v: String) =>
      (ctx.substituteVariablesIn(k), ctx.substituteVariablesIn(v))
    case (k, v: Map[String, Any]) =>
      (ctx.substituteVariablesIn(k), interpolateMap(v, ctx))
    case (k, v: List[Any]) =>
      (ctx.substituteVariablesIn(k), interpolateIterable(v, ctx))
    case (k, v) => (ctx.substituteVariablesIn(k), v)
  }

  implicit class InterpolateOps[T: Interpolate](val t: T) {
    def interpolate(backgroundContext: BackgroundContext): T =
      implicitly[Interpolate[T]].interpolate(t, backgroundContext)
  }
}

trait RecordDataFromFileImplicits {

  implicit def fileKeyValue(implicit
      readHeaders: ReadHeaders[String]
  ): RecordDataFromFile[FileKeyValueRecord] =
    new RecordDataFromFile[FileKeyValueRecord] {
      override def readFromFile(
          t: FileKeyValueRecord,
          f: String => List[String]
      ): List[RecordRead] = {
        f(t.file).map(line => {
          val columns = line.split(t.separator)
          domain.RecordRead(
            t.topicAlias,
            columns(0),
            columns(1),
            Try(columns(2))
              .map(headersString => readHeaders.readHeaders(headersString))
              .getOrElse(Map.empty)
          )
        })
      }
    }

  implicit val fileValueRecord: RecordDataFromFile[FileValueRecord] =
    new RecordDataFromFile[FileValueRecord] {
      override def readFromFile(
          t: FileValueRecord,
          f: String => List[String]
      ): List[RecordRead] = f(t.file).map(line => {
        RecordRead(
          t.topicAlias,
          t.key,
          line,
          Map.empty
        )
      })
    }

  implicit val fileFormattedValue
      : RecordDataFromFile[FileFormattedValueRecord] =
    new RecordDataFromFile[FileFormattedValueRecord] {
      override def readFromFile(
          t: FileFormattedValueRecord,
          f: String => List[String]
      ): List[RecordRead] = {
        val line = f(t.file).map(_.trim).mkString
        List(
          RecordRead(
            t.topicAlias,
            t.key,
            line,
            Map.empty
          )
        )
      }
    }

  implicit class RecordDataFromFileOps[T: RecordDataFromFile](val t: T) {
    def readFromFile(f: String => List[String]): List[RecordRead] =
      implicitly[RecordDataFromFile[T]].readFromFile(t, f)
  }
}

trait RecordDataImplicits {

  implicit val keyValue: RecordData[KeyValueRecord] =
    new RecordData[KeyValueRecord] {
      override def read(t: KeyValueRecord): RecordRead = RecordRead(
        t.topicAlias,
        t.key,
        t.value,
        t.headers
      )
    }

  implicit class RecordDataOps[T: RecordData](val t: T) {
    def read: RecordRead = implicitly[RecordData[T]].read(t)
  }

}
