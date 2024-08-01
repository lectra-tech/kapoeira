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
import Asserts.JsonNodeOps

class JsonNodeOpsTest extends AnyFlatSpec with Matchers {
  behavior of "JsonNodeOps"
  it should "convert to Map[String,Any] for any json object" in {
    val actualSimpleObject = objectMapper.readTree("""{"aString":"bar","aNumber":0.42,"aBoolean":true,"aNull":null}""")
    val expectedSimpleObject = Map("aString" -> "bar", "aNumber" -> 0.42, "aBoolean" -> true ).toSeq

    val actualNestedObject = objectMapper.readTree("""{"foo":{"bar":"baz"}}""")
    val expectedNestedObject = Map("foo" -> Map("bar"-> "baz")).toSeq

    val actualObjectWithArrays = objectMapper.readTree("""{"foo":["item1","item2"]}""")
    val expectedObjectWithArrays = Map("foo" -> Seq("item1","item2")).toSeq

    new JsonNodeOps(actualSimpleObject).toMap.toSeq.intersect(expectedSimpleObject) shouldEqual expectedSimpleObject
    new JsonNodeOps(actualNestedObject).toMap.toSeq.intersect(expectedNestedObject) shouldEqual expectedNestedObject
    new JsonNodeOps(actualObjectWithArrays).toMap.toSeq.intersect(expectedObjectWithArrays) shouldEqual expectedObjectWithArrays

  }
}
