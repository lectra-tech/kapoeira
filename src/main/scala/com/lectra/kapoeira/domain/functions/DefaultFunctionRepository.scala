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
package com.lectra.kapoeira.domain.functions

import java.util.{HexFormat, UUID}
import com.typesafe.scalalogging.LazyLogging

import java.lang.Thread.sleep
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.OffsetDateTime

object DefaultFunctionRepository extends FunctionRepository with LazyLogging {

  def functions: Map[String, Func] = Map(
    "uuid" -> { _ => UUID.randomUUID().toString },
    "uppercase" -> { args => args.head.toUpperCase() },
    "lowercase" -> { args => args.head.toLowerCase() },
    "print" -> { args => logger.info(args.mkString(" ")) },
    "now" -> { _ => OffsetDateTime.now().toString },
    "sleep" -> { args => sleep(args.head.toLong) },
    "sha256" -> {  sha256 _ },
    "sha1" -> {  sha1 _ }
  )

  def sha256(args: Array[String]): String = {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val input = args.mkString(" ")
    messageDigest.update(input.getBytes(StandardCharsets.UTF_8), 0, input.length)
    HexFormat.of().formatHex(messageDigest.digest())
  }

  def sha1(args: Array[String]): String = {
    val messageDigest = MessageDigest.getInstance("SHA-1")
    val input = args.mkString(" ")
    messageDigest.update(input.getBytes(StandardCharsets.UTF_8), 0, input.length)
    HexFormat.of().formatHex(messageDigest.digest())
  }
}
