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

import com.lectra.kapoeira.domain.AssertionContext.{HeadersValue, RecordValue}
import com.lectra.kapoeira.domain.Services.RecordConsumer
import com.lectra.kapoeira.kafka.KapoeiraProducer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.scalamock.scalatest.MockFactory
import org.scalatest
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets
import scala.util.{Failure, Success, Try}

class AssertionContextTest
  extends AnyFeatureSpec
    with Matchers
    with GivenWhenThen
    with MockFactory {

  implicit val recordConsume: RecordConsumer = (_, _) => Map.empty

  Feature("init") {
    Scenario("IAE because of bad background") {
      Given("context")
      val backgroundContext = new BackgroundContext
      val assertionContext = new AssertionContext(WhenStepsLive(backgroundContext, recordConsume, KapoeiraProducer.run _))

      When("init AssertionContext")
      Then("IAE")
      assertThrows[IllegalArgumentException] {
        assertionContext.launchConsumption(
          List(KeyValueWithAliasesRecord("topic", "key", "valueAlias"))
        )
      }
    }

    Scenario("minimum data") {
      Given("minimal background")
      val backgroundContext = mock[BackgroundContext]
      val assertionContext = new AssertionContext(WhenStepsLive(backgroundContext, recordConsume, KapoeiraProducer.run _))
      val consumerRecord = new ConsumerRecord(
        "topic",
        0,
        0,
        "key",
        "value".getBytes.asInstanceOf[Any]
      )
      val keyValueRecord =
        KeyValueWithAliasesRecord("topic", "key", "valueAlias")
      (backgroundContext
        .consumeTopic(_: String, _: Map[String, Int])(_: RecordConsumer))
        .expects(*, *, *)
        .returning(Map("key" -> Seq(consumerRecord)))
      val expectedConsumedRecords = List(keyValueRecord)

      When("init AssertionContext")
      assertionContext.launchConsumption(
        expectedConsumedRecords
      )

      Then("assertionContext maps")
      assertionContext.expectedRecordByValueAlias shouldBe Map(
        "valueAlias" -> keyValueRecord
      )
      assertionContext.expectedRecordsByTopicByKey shouldBe Map(
        "topic" -> Map("key" -> Seq(keyValueRecord))
      )
      assertionContext.consumedRecordsByTopicByKey shouldBe
        Map(
          "topic" -> Map("key" -> Seq(consumerRecord))
        )
    }

    Scenario("two topics, 1 key per topic") {
      Given("background with 2 topics")
      val backgroundContext = mock[BackgroundContext]
      val assertionContext = new AssertionContext(WhenStepsLive(backgroundContext, recordConsume, KapoeiraProducer.run _))
      val recordWithHeaders = new ConsumerRecord(
        "topic1",
        0,
        0,
        "key1",
        "value1.1".getBytes.asInstanceOf[Any]
      )
      recordWithHeaders
        .headers()
        .add("foo", """"bar"""".getBytes(StandardCharsets.UTF_8))
      val consumerRecords1 = Seq(
        recordWithHeaders,
        new ConsumerRecord(
          "topic1",
          0,
          1,
          "key1",
          "value1.2".getBytes.asInstanceOf[Any]
        )
      )
      val consumerRecords2 = Seq(
        new ConsumerRecord(
          "topic2",
          0,
          0,
          "key2",
          "value2".getBytes.asInstanceOf[Any]
        )
      )
      (backgroundContext
        .consumeTopic(_: String, _: Map[String, Int])(_: RecordConsumer))
        .expects("topic1", *, *)
        .returning(Map("key1" -> consumerRecords1))
      (backgroundContext
        .consumeTopic(_: String, _: Map[String, Int])(_: RecordConsumer))
        .expects("topic2", *, *)
        .returning(Map("key2" -> consumerRecords2))
      val expectedConsumedRecords = List(
        KeyValueWithAliasesRecord(
          "topic1",
          "key1",
          "alias_value1.1",
          Some("aliasHeaders1.1")
        ),
        KeyValueWithAliasesRecord(
          "topic2",
          "key2",
          "alias_value2",
          Some("aliasHeaders2")
        ),
        KeyValueWithAliasesRecord(
          "topic1",
          "key1",
          "alias_value1.2",
          Some("aliasHeaders1.2")
        )
      )

      When("init AssertionContext")
      assertionContext.launchConsumption(
        expectedConsumedRecords
      )

      Then("assertionContext maps")
      assertionContext.expectedRecordByValueAlias shouldBe Map(
        "alias_value1.1" -> expectedConsumedRecords.head,
        "alias_value2" -> expectedConsumedRecords(1),
        "alias_value1.2" -> expectedConsumedRecords(2)
      )
      assertionContext.expectedRecordByHeadersAlias shouldBe Map(
        "aliasHeaders1.1" -> expectedConsumedRecords.head,
        "aliasHeaders2" -> expectedConsumedRecords(1),
        "aliasHeaders1.2" -> expectedConsumedRecords(2)
      )
      assertionContext.expectedRecordsByTopicByKey shouldBe Map(
        "" +
          "topic1" -> Map(
          "key1" -> Seq(
            expectedConsumedRecords.head,
            expectedConsumedRecords(2)
          )
        ),
        "topic2" -> Map("key2" -> Seq(expectedConsumedRecords(1)))
      )
      assertionContext.consumedRecordsByTopicByKey shouldBe Map(
        "topic1" -> Map("key1" -> consumerRecords1),
        "topic2" -> Map("key2" -> consumerRecords2)
      )

      And("extracting consumed record by headers alias")
      assertionContext.extractConsumedRecordWithAlias(
        "alias_value1.1"
      ) shouldBe Some(
        AssertionContext.RecordValue(consumerRecords1.head.value())
      )
      val Some(AssertionContext.HeadersValue(headers)) =
        assertionContext.extractConsumedRecordWithAlias(
          "aliasHeaders1.1"
        )
      headers.map { case (k, v) => (k, new String(v)) } shouldBe Map(
        "foo" -> """"bar""""
      )
    }

    Scenario("No header alias nor value alias found") {
      Given("background with 2 topics")
      val backgroundContext = mock[BackgroundContext]
      val assertionContext = new AssertionContext(WhenStepsLive(backgroundContext, recordConsume, KapoeiraProducer.run _))
      val recordWithHeaders = new ConsumerRecord(
        "topic2",
        0,
        0,
        "key2",
        "value1.2".getBytes.asInstanceOf[Any]
      )
      recordWithHeaders
        .headers()
        .add("foo", """"bar"""".getBytes(StandardCharsets.UTF_8))
      val consumerRecords2 = Seq(
        recordWithHeaders,
        new ConsumerRecord(
          "topic2",
          0,
          1,
          "key2",
          "value1.2".getBytes.asInstanceOf[Any]
        )
      )
      val expectedConsumedRecords = List(
        KeyValueWithAliasesRecord(
          "topic1",
          "key1",
          "alias_value1.1",
          Some("aliasHeaders1.1")
        ),
        KeyValueWithAliasesRecord(
          "topic2",
          "key2",
          "alias_value1.2",
          Some("aliasHeaders1.2")
        )
      )

      (backgroundContext
        .consumeTopic(_: String, _: Map[String, Int])(_: RecordConsumer))
        .expects("topic1", *, *)
        .returning(Map())
      (backgroundContext
        .consumeTopic(_: String, _: Map[String, Int])(_: RecordConsumer))
        .expects("topic2", *, *)
        .returning(Map("key2" -> consumerRecords2))

      When("init AssertionContext")
      assertionContext.launchConsumption(
        expectedConsumedRecords
      )

      Then("extracting consumed record by headers alias")
      Try(assertionContext.extractConsumedRecordWithAlias("alias_value1.1")) match {
        case Failure(exception) => exception.getMessage shouldEqual "Alias alias_value1.1 not found in header alias context and Expected key key1 with alias_value1.1 or aliasHeaders1.1 alias not found in Map()"
        case Success(_) => fail("an exception should have been thrown")
      }
      Try(assertionContext.extractConsumedRecordWithAlias("alias_value1.2")) match {
        case Failure(exception) => fail(exception)
        case Success(Some(RecordValue(_))) => scalatest.Assertions.succeed
        case v => fail(v.toString)
      }
      Try(assertionContext.extractConsumedRecordWithAlias("aliasHeaders1.2")) match {
        case Failure(exception) => fail(exception)
        case Success(Some(HeadersValue(_))) => scalatest.Assertions.succeed
        case v => fail(v.toString)
      }
    }

    Scenario("1 topic, 2 keys") {
      Given("background with 1 topic")
      val backgroundContext = mock[BackgroundContext]
      val assertionContext = new AssertionContext(WhenStepsLive(backgroundContext, recordConsume, KapoeiraProducer.run _))
      val consumerRecordsKey1 = Seq(
        new ConsumerRecord(
          "topic1",
          0,
          0,
          "key1",
          "value1.1".getBytes.asInstanceOf[Any]
        ),
        new ConsumerRecord(
          "topic1",
          0,
          1,
          "key1",
          "value1.2".getBytes.asInstanceOf[Any]
        )
      )
      val consumerRecordsKey2 = Seq(
        new ConsumerRecord(
          "topic2",
          1,
          0,
          "key2",
          "value2".getBytes.asInstanceOf[Any]
        )
      )
      (backgroundContext
        .consumeTopic(_: String, _: Map[String, Int])(_: RecordConsumer))
        .expects("topic1", *, *)
        .returning(
          Map(
            "key1" -> consumerRecordsKey1,
            "key2" -> consumerRecordsKey2
          )
        )
      val expectedConsumedRecords = List(
        KeyValueWithAliasesRecord("topic1", "key1", "alias_value1.1"),
        KeyValueWithAliasesRecord("topic1", "key2", "alias_value2"),
        KeyValueWithAliasesRecord("topic1", "key1", "alias_value1.2")
      )

      When("init AssertionContext")
      assertionContext.launchConsumption(
        expectedConsumedRecords
      )

      Then("assertionContext maps")
      assertionContext.expectedRecordByValueAlias shouldBe Map(
        "alias_value1.1" -> expectedConsumedRecords.head,
        "alias_value2" -> expectedConsumedRecords(1),
        "alias_value1.2" -> expectedConsumedRecords(2)
      )
      assertionContext.expectedRecordsByTopicByKey shouldBe Map(
        "topic1" -> Map(
          "key1" -> Seq(
            expectedConsumedRecords.head,
            expectedConsumedRecords(2)
          ),
          "key2" -> Seq(expectedConsumedRecords(1))
        )
      )
      assertionContext.consumedRecordsByTopicByKey shouldBe Map(
        "topic1" -> Map(
          "key1" -> consumerRecordsKey1,
          "key2" -> consumerRecordsKey2
        )
      )
    }
  }

}
