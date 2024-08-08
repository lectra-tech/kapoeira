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

import com.lectra.kapoeira.domain.{BackgroundContext, RecordRead}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets

object InterpolateTest extends Properties("String") {
  val ctx = new BackgroundContext

  property("identity when context is empty") = forAll { (a: String) =>
    a.interpolate(ctx) == a
  }
}

class InterpolateTest extends AnyFlatSpec with Matchers {
  behavior of "interpolate"
  it should "interpolate variables of a string" in {
    val ctx = new BackgroundContext
    ctx.addVariable("key1", "foo")
    ctx.addVariable("key2", "bar")
    val template = """${key1} is a ${key2}"""

    template.interpolate(ctx) shouldBe "foo is a bar"
  }

  it should "interpolate variables of a recursive Map structure" in {
    val ctx = new BackgroundContext
    ctx.addVariable("key1", "foo")
    ctx.addVariable("key2", "bar")

    val templateHeaders: Map[String, Any] = Map(
      "${key1}" -> "${key2}",
      "baz" -> Map(
        "${key1}" -> 42,
        "qux" -> List("quz", "${key2}")
      )
    )
    val recordRead = RecordRead(
      "${key1}Topic",
      "${key2}Key",
      "${key1}Value",
      templateHeaders
    )

    val RecordRead(
      topic,
      key,
      value,
      headers
    ) = recordRead.interpolate(ctx)

    topic shouldBe "fooTopic"
    key shouldBe "barKey"
    new String(value) shouldBe "fooValue"
    headers shouldBe Map(
      "foo" -> "bar",
      "baz" -> Map(
        "foo" -> 42,
        "qux" -> List("quz", "bar")
      )
    )
  }
}
