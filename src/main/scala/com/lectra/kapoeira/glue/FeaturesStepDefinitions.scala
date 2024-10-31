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

import com.lectra.kapoeira.domain.CallScript._
import com.lectra.kapoeira.domain.functions.DefaultFunctionRepository
import com.lectra.kapoeira.domain.{AssertionContext, BackgroundContext, CallScript, WhenStepsLive}
import com.lectra.kapoeira.glue.Asserts.{JsonExpr, JsonNodeOps}
import com.lectra.kapoeira.glue.DataTableParser._
import com.lectra.kapoeira.kafka.KapoeiraProducer
import com.typesafe.scalalogging.LazyLogging
import io.cucumber.datatable.DataTable
import io.cucumber.scala.{EN, ScalaDsl}
import org.scalatest.matchers.should.Matchers

class FeaturesStepDefinitions
    extends ScalaDsl
    with EN
    with Matchers
    with LazyLogging {

  private implicit val backgroundContext: BackgroundContext =
    new BackgroundContext()

  private val assertionContext =
    new AssertionContext(WhenStepsLive(backgroundContext,kafkaConsume,KapoeiraProducer.run _))
  private val defaultFuncRepository = DefaultFunctionRepository
  private val functionManger = new FunctionManager(defaultFuncRepository)

  // Background
  Given("^input\\s+topic$") { data: DataTable =>
    ConsoleTimer.time(
      "inputTopic", {
        parseInputTopicDataTable(data).foreach(backgroundContext.addInput)
      }
    )
  }

  Given("^output\\s+topic$") { data: DataTable =>
    ConsoleTimer.time(
      "outputTopic", {
        parseOutputTopicDataTable(data).foreach(backgroundContext.addOutput)
      }
    )
  }

  // STATIC VAR
  Given(
    "^var\\s+(.*)\\s+=\\s+(?!call\\s+function\\s*:|call\\s+script\\s*:)(.*)$"
  ) { (variableName: String, postEqualDefinition: String) =>
    backgroundContext.addVariable(variableName, postEqualDefinition)
  }

  // FUNCTION
  Given("^var\\s+(.*)\\s+=\\s+call\\s+function\\s*:\\s*(\\S+)\\s*(.*)$") {
    (variableName: String, functionDefinition: String, params: String) =>
      {
        functionManger(variableName, functionDefinition, backgroundContext.substituteVariablesIn(params))
          .fold(fail(_), identity)
      }
  }

  // SUBJECT
  Given("^subject$") { data: DataTable =>
    parseSubjectDataTable(data).foreach(backgroundContext.addSubject)
  }

  // Scenario

  // PRODUCE

  When(
    "^records\\s+from\\s+file\\s+with\\s+key\\s+and\\s+value\\s+are\\s+sent\\s*$"
  ) { records: DataTable =>
    val batches = parseFileKeyValueDataTable(records)
      .map(r =>
        (
          r.batch,
          r.readFromFile(openFile(_)).map(_.interpolate(backgroundContext))
        )
      )
    assertionContext.registerWhen(batches)
  }

  When("^records\\s+from\\s+file\\s+with\\s+value\\s+are\\s+sent\\s*$") {
    records: DataTable =>
      val batches = parseFileValueDataTable(records).map(r =>
        (
          r.batch,
          r.readFromFile(openFile(_)).map(_.interpolate(backgroundContext))
        )
      )
      assertionContext.registerWhen(batches)
  }

  When(
    "^records\\s+from\\s+file\\s+with\\s+formatted\\s+value\\s+are\\s+sent\\s*$"
  ) { records: DataTable =>
    val batches = parseFileFormattedValueDataTable(records)
      .map(r =>
        (
          r.batch,
          r.readFromFile(openFile(_)).map(_.interpolate(backgroundContext))
        )
      )
    assertionContext.registerWhen(batches)
  }

  When("^records\\s+with\\s+key\\s+and\\s+value\\s+are\\s+sent\\s*$") {
    records: DataTable =>
      val batches = parseKeyValueDataTable(records)
        .map(r=>(
          r.batch,
          List(r.read.interpolate(backgroundContext))
        ))
      assertionContext.registerWhen(batches)
  }

  // CONSUME
  Then("^expected\\s+records\\s*$") { messages: DataTable =>
    ConsoleTimer.time(
      "runConsume", {
        try {
          logger.debug("Expected records step")
          assertionContext.launchConsumption(
            parseKeyValueAliasesDataTable(messages).map(kv =>
              kv.copy(key = backgroundContext.substituteVariablesIn(kv.key))
            )
          )
          logger.debug(assertionContext.showConsumedRecords)
        } finally {
          backgroundContext.close()
        }
      }
    )
  }

  // CALL EXTERNAL TOOLING
  When("^call\\s+script\\s*:\\s+(.+)") { script: String =>
    assertionContext.registerWhenScript(CallScriptPath(script))
  }

  And("^var\\s+(.*)\\s+=\\s+call\\s+script\\s*:\\s+(.+)$") {
    (variableName: String, script: String) =>
      val result = CallScriptPath(script).run(backgroundContext)
      result.exitCode shouldBe 0
      backgroundContext.addVariable(variableName, result.stdOut)
  }

  //DOCSTRING version
  When("^call\\s+script\\s+:") { script: String =>
    assertionContext.registerWhenScript(CallPlainScript(script))
  }

  And("^var\\s+(.*)\\s+=\\s+call\\s+script\\s*:$") {
    (variableName: String, script: String) =>
      val result = CallPlainScript(script).run(backgroundContext)
      result.exitCode shouldBe 0
      backgroundContext.addVariable(variableName, result.stdOut)
  }

  // ASSERT
  And("^assert\\s+(\\S+)\\s+(\\S+)\\s*==\\s*([^+-]*)\\s*$") {
    (alias: String, jsonExpression: String, expected: String) =>
      logger.debug(
        s"Assert Step : (alias,jsonExpression,expected) ($alias,$jsonExpression,$expected)"
      )
      logger.debug(assertionContext.showConsumedRecords)
      // TODO capture operator vs several And expression
      val interpolated = backgroundContext.substituteVariablesIn(expected)
      logger.debug(
        s"""alias: $alias, jsonExpression: $jsonExpression, expected: $interpolated"""
      )
      Asserts.equal(assertionContext, alias, jsonExpression, interpolated)
  }

  And("^assert\\s+(\\S+)\\s+(\\S+)\\s+match\\s+object\\s+(\\{.*\\})\\s*$") {
    (alias: String, jsonExpression: String, expectedJsonObject: String) =>
      val interpolated =
        backgroundContext.substituteVariablesIn(expectedJsonObject)
      Asserts.matchObject(assertionContext, alias, jsonExpression, interpolated)
  }

  And("^assert\\s+(\\S+)\\s+(\\S+)\\s+match\\s+exact\\s+object\\s+(\\{.*\\})\\s*$") {
    (alias: String, jsonExpression: String, expectedJsonObject: String) =>
      val interpolated =
        backgroundContext.substituteVariablesIn(expectedJsonObject)
      Asserts.matchExactObject(assertionContext, alias, jsonExpression, interpolated)
  }

  And("^assert\\s+(\\S+)\\s+(\\$\\S*)\\s+has size\\s+(\\d*)$") {
    (alias: String, jsonExpression: String, expectedSize: Long) =>
      Asserts.assertKafkaOutput(
        assertionContext,
        alias,
        jsonExpression,
        { actual => actual should have size expectedSize }
      )
  }

  And("^assert\\s+var\\s+(\\S+)\\s+(\\$\\S*)\\s*==\\s+(.*)$") {
    (variableName: String, jsonExpression: String, expectedJson: String) =>
      val interpolatedExpectedJson =
        backgroundContext.substituteVariablesIn(expectedJson)
      val variable = backgroundContext.getVariable(variableName)
      Asserts.assertJson(
        jsonExpression,
        { actual => assert(actual == JsonExpr(interpolatedExpectedJson).value) },
        variable.get
      )
  }

  And("""^assert\s+(\S+)\s+(\$\S*)\s*==\s+([+-.eE0-9]+)\s+\+\-\s+([+-.eE0-9]+)\s*$""") {
    (alias: String, jsonExpression: String, expectedJsonNumber: String,approximationJsonNumber:String) =>
      val interpolatedExpectedJson =
        backgroundContext.substituteVariablesIn(expectedJsonNumber)
      val interpolatedApproximationJson =
        backgroundContext.substituteVariablesIn(approximationJsonNumber)
      Asserts.approxEqual(
        assertionContext,
        alias,
        jsonExpression,
        interpolatedExpectedJson,
        interpolatedApproximationJson
      )
  }

  And("^assert\\s+var\\s+(\\S+)\\s+(\\$\\S*)\\s+match\\s+object\\s+(.*)$") {
    (variableName: String, jsonExpression: String, expectedJson: String) =>
      val interpolatedExpectedJson =
        backgroundContext.substituteVariablesIn(expectedJson)
      val variable = backgroundContext.getVariable(variableName)
      Asserts.assertJson(
        jsonExpression,
        { actual =>
          val actualSet = actual.toMap.toSet
          val expectedSet = JsonExpr(interpolatedExpectedJson).value.toMap.toSet
          assert(expectedSet.intersect(actualSet) == expectedSet,s"Actual json $actual does not match $expectedJson")
        },
        variable.get
      )
  }

  And("^assert\\s+var\\s+(\\S+)\\s+(\\$\\S*)\\s+match\\s+exact\\s+object\\s+(.*)$") {
    (variableName: String, jsonExpression: String, expectedJson: String) =>
      val interpolatedExpectedJson =
        backgroundContext.substituteVariablesIn(expectedJson)
      val variable = backgroundContext.getVariable(variableName)
      Asserts.assertJson(
        jsonExpression,
        { actual =>
          val actualSet = actual.toMap.toSet
          val expectedSet = JsonExpr(interpolatedExpectedJson).value.toMap.toSet
          assert(actualSet == expectedSet, s"Actual json $actual does not match $expectedJson")
        },
        variable.get
      )
  }

  And("^assert\\s+var\\s+(\\S+)\\s+(\\$\\S*)\\s+has size\\s+(\\d*)$") {
    (variableName: String, jsonExpression: String, expectedSize: Long) =>
      val variable = backgroundContext.getVariable(variableName)
      Asserts.assertJson(
        jsonExpression,
        { actual => actual should have size expectedSize },
        variable.get
      )
  }

  And("^match\\s+(\\S+)\\s*==\\s*(\\{.*\\}|\\[.*\\])$") {
    (output: String, expected: String) =>
      logger.info(output)
      logger.info(expected)
  }

}
