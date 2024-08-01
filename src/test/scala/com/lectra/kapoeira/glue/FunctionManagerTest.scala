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

import com.lectra.kapoeira.domain.BackgroundContext
import com.lectra.kapoeira.domain.functions.{Func, FunctionRepository, DefaultFunctionRepository}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FunctionManagerTest extends AnyFlatSpec with Matchers {

  private val testRepo = new FunctionRepository {
    override def functions: Map[String, Func] = Map(
      "max" -> { case Array(fst, snd) =>
        val (intFst, intSnd) = (fst.toInt, snd.toInt)
        if(intFst > intSnd) intFst else intSnd
      }
    )
  }

  it should "parse parameters correctly and resolve variables" in {
    implicit val context: BackgroundContext = new BackgroundContext()
    val functionManager = new FunctionManager(DefaultFunctionRepository)
    context.addVariable("foo", "hello world")
    val result = functionManager.resolveVariables("42 coucou ${foo}")

    assert(result.isRight)
    result.fold(err => fail(err.toString), variable => assert(variable == List("42", "coucou", "hello world")))
  }

  it should "apply non variable parameters to existing function and successfully updates the context" in {
    implicit val context: BackgroundContext = new BackgroundContext()

    val functionDef = "max"
    val paramDef = "33 42"
    val functionManager = new FunctionManager(testRepo)
    val result = functionManager("maxValue", functionDef, paramDef)
    result match {
      case Right(_) =>
        context.getVariable("maxValue")
        .foreach(value => assert(value == "42"))
      case Left(reason) =>
        fail(reason)

    }
  }

  it should "successfully resolve variables and apply function, and updates the context" in {
    implicit val context: BackgroundContext = new BackgroundContext()
    context.addVariable("foo", "33")
    context.addVariable("bar", "43")

    val functionDef = "max"
    val paramDef = "${foo} ${bar}"
    val functionManager = new FunctionManager(testRepo)
    val result = functionManager("maxValue", functionDef, paramDef)
    result match {
      case Right(_) =>
        context.getVariable("maxValue")
          .foreach(value => assert(value == "43"))
      case Left(reason) =>
        fail(reason)
    }
  }

  it should "return an error explaining that a params hasn't been resolved" in {
    implicit val context: BackgroundContext = new BackgroundContext()
    context.addVariable("foo", "33")
    val functionDef = "max"
    val paramDef = "${foo} ${bar}"
    val functionManager = new FunctionManager(testRepo)
    val result = functionManager("maxValue", functionDef, paramDef)
    result match {
      case Right(_) =>
        fail("result should be left")
      case Left(reason) =>
        succeed
    }
  }
}
