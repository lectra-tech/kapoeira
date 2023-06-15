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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BackgroundContextTest extends AnyFlatSpec with Matchers {
  "substitute variables" should "replace in a string all <variable> by its value" in {
    val template = "some ${foo} with ${bar}"
    val context = new BackgroundContext
    context.addVariable("foo","a value")
    context.addVariable("bar","42")
    val actual = context.substituteVariablesIn(template)

    actual shouldEqual "some a value with 42"
  }

  it should "not replace in a string not corresponding key" in {
    val template = "some ${} with ${x} finally"
    val context = new BackgroundContext
    context.addVariable("foo","a value")
    context.addVariable("bar","42")
    val actual = context.substituteVariablesIn(template)

    actual shouldEqual template
  }
}
