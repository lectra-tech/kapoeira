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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReadHeadersTest extends AnyFlatSpec with Matchers {
  behavior of "read headers from string"

  it should "parse a string to a scala Map[String,Any]" in {
    val aJson =
      """
        |{"foo":"bar","baz":42,"qux":false,"buzz":{"toto":"titi"}}
        |""".stripMargin

    val actual = aJson.readHeaders
    actual should contain theSameElementsAs Map(
      "foo" -> "bar",
      "baz" -> 42,
      "qux" -> false,
      "buzz" -> Map(
        "toto" -> "titi"
      )
    )
  }
}
