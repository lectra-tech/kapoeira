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
package com.lectra.kapoeira.glue

import com.lectra.kapoeira.domain.Services.RecordConsumer
import com.lectra.kapoeira.domain.{AssertionContext, BackgroundContext, KeyValueRecord, KeyValueWithAliasesRecord, WhenStepsLive}
import com.lectra.kapoeira.kafka.KapoeiraProducer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.scalamock.scalatest.MockFactory
import org.scalatest.exceptions.TestFailedException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AssertsTest extends AnyFlatSpec with Matchers with MockFactory {

  val recordConsume: RecordConsumer = (_, _) => Map.empty

  behavior of "Asserts"

  it should "assert approximatively equals on numbers" in {
    val backgroundContext = mock[BackgroundContext]
    val assertionContext = new AssertionContext(WhenStepsLive(backgroundContext, recordConsume, KapoeiraProducer.run _))
    val consumerRecord =
      new ConsumerRecord("topic", 0, 0, "key", """{"foo": 12.003820965382393}""".getBytes.asInstanceOf[Any])
    val valueAlias = "valueAlias"
    val keyValueRecord = KeyValueWithAliasesRecord("topic", "key", valueAlias)
    (backgroundContext
      .consumeTopic(_: String, _: Map[String, Int])(_: RecordConsumer))
      .expects(*, *, *)
      .returning(Map("key" -> Seq(consumerRecord)))
    val expectedConsumedRecords = List(keyValueRecord)
    assertionContext.launchConsumption(expectedConsumedRecords)

    Asserts.approxEqual(assertionContext, valueAlias, "$.foo", "12.0038","0.0001" )
    Asserts.approxEqual(assertionContext, valueAlias, "$.foo", "12.0038","1E-4" )
    Asserts.approxEqual(assertionContext, valueAlias, "$.foo", "12.0037","1E-3" )
    Asserts.approxEqual(assertionContext, valueAlias, "$.foo", "12.0039","1E-3" )
    Asserts.approxEqual(assertionContext, valueAlias, "$.foo", "12.003","1E-3" )
    Asserts.approxEqual(assertionContext, valueAlias, "$.foo", "12.004","1E-3" )
    Asserts.approxEqual(assertionContext, valueAlias, "$.foo", "12.0","0.1" )
    Asserts.approxEqual(assertionContext, valueAlias, "$.foo", "12.1","0.1" )

  }

  it should "assert equality on literals" in {
    val backgroundContext = mock[BackgroundContext]
    val assertionContext = new AssertionContext(WhenStepsLive(backgroundContext, recordConsume, KapoeiraProducer.run _))
    val consumerRecord =
      new ConsumerRecord("topic", 0, 0, "key", """{"foo":"bar"}""".getBytes.asInstanceOf[Any])
    val valueAlias = "valueAlias"
    val keyValueRecord = KeyValueWithAliasesRecord("topic", "key", valueAlias)
    (backgroundContext
      .consumeTopic(_: String, _: Map[String, Int])(_: RecordConsumer))
      .expects(*, *, *)
      .returning(Map("key" -> Seq(consumerRecord)))
    val expectedConsumedRecords = List(keyValueRecord)
    assertionContext.launchConsumption(expectedConsumedRecords)

    Asserts.equal(assertionContext, valueAlias, "$.foo", "\"bar\"")
  }

  it should "assert matching objects" in {
    val backgroundContext = mock[BackgroundContext]
    val assertionContext = new AssertionContext(WhenStepsLive(backgroundContext, recordConsume, KapoeiraProducer.run _))
    val consumerRecord = new ConsumerRecord(
      "topic",
      0,
      0,
      "key",
      """{"foo":"bar","baz":{"qux":42,"quux":"corge"}}""".getBytes.asInstanceOf[Any]
    )
    val valueAlias = "valueAlias"
    val keyValueRecord = KeyValueWithAliasesRecord("topic", "key", valueAlias)
    (backgroundContext
      .consumeTopic(_: String, _: Map[String, Int])(_: RecordConsumer))
      .expects(*, *, *)
      .returning(Map("key" -> Seq(consumerRecord)))
    val expectedConsumedRecords = List(keyValueRecord)
    assertionContext.launchConsumption(expectedConsumedRecords)

    Asserts.matchObject(
      assertionContext,
      valueAlias,
      "$",
      """{"foo":"bar","baz":{"qux":42,"quux":"corge"}}"""
    )
    Asserts.matchObject(
      assertionContext,
      valueAlias,
      "$",
      """{"foo":"bar"}"""
    )
    assertThrows[TestFailedException](
      Asserts.matchObject(
        assertionContext,
        valueAlias,
        "$",
        """{"foo":"bar","baz":{"qux":42,"quux":"corge","gralpy":false},"grault":true}"""
      )
    )
  }

  it should "assert that two JSON objects are exactly equal" in {
    val backgroundContext = mock[BackgroundContext]
    val assertionContext = new AssertionContext(WhenStepsLive(backgroundContext, recordConsume, KapoeiraProducer.run _))
    val consumerRecord = new ConsumerRecord(
      "topic",
      0,
      0,
      "key",
      """{"foo":"bar","baz":{"qux":42,"quux":"corge"}}""".getBytes.asInstanceOf[Any]
    )
    val valueAlias = "valueAlias"
    val keyValueRecord = KeyValueWithAliasesRecord("topic", "key", valueAlias)
    (backgroundContext
      .consumeTopic(_: String, _: Map[String, Int])(_: RecordConsumer))
      .expects(*, *, *)
      .returning(Map("key" -> Seq(consumerRecord)))
    val expectedConsumedRecords = List(keyValueRecord)
    assertionContext.launchConsumption(expectedConsumedRecords)

    Asserts.matchExactObject(
      assertionContext,
      valueAlias,
      "$",
      """{"foo":"bar","baz":{"qux":42,"quux":"corge"}}"""
    )
    Asserts.matchExactObject(
      assertionContext,
      valueAlias,
      "$",
      """{"baz":{"qux":42,"quux":"corge"},"foo":"bar"}"""
    )
    assertThrows[TestFailedException](
      Asserts.matchExactObject(
        assertionContext,
        valueAlias,
        "$",
        """{"foo":"bar"}"""
      )
    )
    assertThrows[TestFailedException](
      Asserts.matchExactObject(
        assertionContext,
        valueAlias,
        "$",
        """{"foo":"bar","baz":{"qux":42,"quux":"corge","gralpy":false},"grault":true}"""
      )
    )
  }
}
