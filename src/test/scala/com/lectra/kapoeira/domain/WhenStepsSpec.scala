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

import com.lectra.kapoeira.domain.Services.{RecordConsumer, RecordProducer}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.ZIO
import zio.test.Assertion._
import zio.test.{ZIOSpecDefault, _}

object WhenStepsSpec extends ZIOSpecDefault {

  val aTopic = "aTopic"
  val aTopic2 = "aTopic2"
  val aTopicAlias = "aTopicAlias"
  val aKey = "aKey"

  val spec = suite("Handle run of when steps")(
    suite("sending record, and consume")(
      test("one record") {
        //prepare
        val aValue = "aValue"

        val backgroundContext: BackgroundContext = buildBackgroundContext
        val kafkaStubb = new KafkaStubb
        val whenStepsService =
          WhenStepsLive(
            backgroundContext,
            kafkaStubb.consumer(),
            kafkaStubb.producer()
          )
        val steps = whenStepsService.registerWhen(
          WhenStep.empty,
          List(
            0 -> List(
              RecordRead(aTopicAlias, aKey, aValue, Map.empty)
            )
          )
        )

        //run
        for {
          res <- whenStepsService
            .run(
              steps,
              List(KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias"))
            )
            .map(_.map { case (t, vs) =>
              (t, vs.map { case (k, v) => (k, v.map(_.value())) })
            })
        } yield (
          //assert
          assert(res(aTopicAlias)(aKey))(hasSameElements(Seq(aValue)))
          )
      },
      test("one batch of many records, received in order") {
        //prepare
        val backgroundContext: BackgroundContext = buildBackgroundContext
        val kafkaStubb = new KafkaStubb
        val whenStepsService =
          WhenStepsLive(
            backgroundContext,
            kafkaStubb.consumer(),
            kafkaStubb.producer()
          )
        val steps = whenStepsService.registerWhen(
          WhenStep.empty,
          List(
            0 -> List(
              RecordRead(aTopicAlias, aKey, "aValue1", Map.empty),
              RecordRead(aTopicAlias, aKey, "aValue2", Map.empty),
              RecordRead(aTopicAlias, aKey, "aValue3", Map.empty),
              RecordRead(aTopicAlias, aKey, "aValue4", Map.empty)
            )
          )
        )

        //run
        for {
          res <- whenStepsService
            .run(
              steps,
              List(
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias1"),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias2"),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias3"),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias4")
              )
            )
            .map(_.map { case (t, vs) =>
              (t, vs.map { case (k, v) => (k, v.map(_.value())) })
            })
        } yield (
          //assert
          assert(res(aTopicAlias)(aKey))(
            hasSameElements(Seq("aValue1", "aValue2", "aValue3", "aValue4"))
          )
          )
      },
      test("two batches of many records, received in order") {
        //prepare
        val backgroundContext: BackgroundContext = buildBackgroundContext
        val kafkaStubb = new KafkaStubb
        val whenStepsService =
          WhenStepsLive(
            backgroundContext,
            kafkaStubb.consumer(),
            kafkaStubb.producer()
          )
        val steps = whenStepsService.registerWhen(
          WhenStep.empty,
          List(
            1 -> List(
              RecordRead(aTopicAlias, aKey, "aValue1", Map.empty),
              RecordRead(aTopicAlias, aKey, "aValue2", Map.empty)
            ),
            2 -> List(
              RecordRead(aTopicAlias, aKey, "aValue3", Map.empty),
              RecordRead(aTopicAlias, aKey, "aValue4", Map.empty)
            )
          )
        )

        //run
        for {
          res <- whenStepsService
            .run(
              steps,
              List(
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias1", None, 1),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias2", None, 1),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias3", None, 2),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias4", None, 2)
              )
            )
            .map(_.map { case (t, vs) =>
              (t, vs.map { case (k, v) => (k, v.map(_.value())) })
            })
        } yield (
          //assert
          assert(res.get(aTopicAlias))(isSome) &&
            assert(res(aTopicAlias).get(aKey))(
              isSome(
                hasSameElements(Seq("aValue1", "aValue2", "aValue3", "aValue4"))
              )
            )
          )
      },
      test("register a generic step that will send a record too") {
        //prepare
        val backgroundContext: BackgroundContext = buildBackgroundContext
        val kafkaStubb = new KafkaStubb
        val whenStepsService =
          WhenStepsLive(
            backgroundContext,
            kafkaStubb.consumer(),
            kafkaStubb.producer()
          )
        val steps = whenStepsService.registerWhen(
          WhenStep.empty,
          List(
            1 -> List(
              RecordRead(aTopicAlias, aKey, "aValue1", Map.empty),
              RecordRead(aTopicAlias, aKey, "aValue2", Map.empty)
            ),
            2 -> List(
              RecordRead(aTopicAlias, aKey, "aValue3", Map.empty),
              RecordRead(aTopicAlias, aKey, "aValue4", Map.empty)
            )
          )
        ).addStepOnLastBatch(
          kafkaStubb.producer().run(
            RecordRead(aTopicAlias, aKey, "aValue5", Map.empty),
            backgroundContext.inputTopicConfigs.apply(aTopicAlias),
            None,
            None
          )
        )

        //run
        for {
          res <- whenStepsService
            .run(
              steps,
              List(
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias1", None, 1),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias2", None, 1),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias3", None, 2),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias4", None, 2),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias5", None, 2)
              )
            )
            .map(_.map { case (t, vs) =>
              (t, vs.map { case (k, v) => (k, v.map(_.value())) })
            })
        } yield (
          //assert
          assert(res.get(aTopicAlias))(isSome) &&
            assert(res(aTopicAlias).get(aKey))(
              isSome(
                hasSameElements(Seq("aValue1", "aValue2", "aValue3", "aValue4", "aValue5"))
              )
            )
          )
      },
      test("register a CallScript object") {
        //prepare
        val backgroundContext: BackgroundContext = buildBackgroundContext
        val kafkaStubb = new KafkaStubb
        val whenStepsService =
          WhenStepsLive(
            backgroundContext,
            kafkaStubb.consumer(),
            kafkaStubb.producer()
          )
        val firstSteps = whenStepsService.registerWhen(
          WhenStep.empty,
          List(
            1 -> List(
              RecordRead(aTopicAlias, aKey, "aValue1", Map.empty),
              RecordRead(aTopicAlias, aKey, "aValue2", Map.empty)
            ),
            2 -> List(
              RecordRead(aTopicAlias, aKey, "aValue3", Map.empty),
              RecordRead(aTopicAlias, aKey, "aValue4", Map.empty)
            )
          )
        )

        val steps = whenStepsService.registerWhenScript(firstSteps,CallScript.CallPlainScript("echo 42"))

        //run
        for {
          res <- whenStepsService
            .run(
              steps,
              List(
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias1", None, 1),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias2", None, 1),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias3", None, 2),
                KeyValueWithAliasesRecord(aTopicAlias, aKey, "valueAlias4", None, 2),
              )
            )
            .map(_.map { case (t, vs) =>
              (t, vs.map { case (k, v) => (k, v.map(_.value())) })
            })
        } yield (
          //assert
          assert(res.get(aTopicAlias))(isSome) &&
            assert(res(aTopicAlias).get(aKey))(
              isSome(
                hasSameElements(Seq("aValue1", "aValue2", "aValue3", "aValue4"))
              )
            )
          )
      }
    )
  )

  final class KafkaStubb {
    var records: Map[String, Seq[ConsumerRecord[String, Any]]] = Map.empty

    def consumer(): RecordConsumer = (_, _) => {
      val res = collection.immutable.Map.from(records)
      records = Map.empty //simulate an offset commit
      res
    }

    def producer(): RecordProducer = (record, topicConfig, _, _) =>
      ZIO.succeed {
        records = records.updated(
          record.key,
          records
            .getOrElse(record.key, Seq.empty) :+ new ConsumerRecord[String, Any](
            topicConfig.topicName,
            0,
            0,
            record.key,
            new String(record.value)
          )
        )
      }
  }

  private def buildBackgroundContext = {
    val backgroundContext = new BackgroundContext
    backgroundContext.addInput(
      InputTopicConfig(aTopic, aTopicAlias, "string", "string")
    )
    backgroundContext.addOutput(
      OutputTopicConfig(aTopic, aTopicAlias, "string", "string")
    )(
      (
        outputTopicConfig: OutputTopicConfig,
        subjectConfigs: Map[String, SubjectConfig]
      ) => OutputConfigStringString(outputTopicConfig, null)
    )
    backgroundContext
  }
}
