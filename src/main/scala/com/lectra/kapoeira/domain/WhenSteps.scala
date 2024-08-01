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
package com.lectra.kapoeira.domain

import com.lectra.kapoeira.domain.MergeMaps._
import com.lectra.kapoeira.domain.Services.{RecordConsumer, RecordProducer}
import com.lectra.kapoeira.domain.WhenStep.WhenStepRuntime
import com.lectra.kapoeira.domain.WhenSteps._
import com.lectra.kapoeira.glue.ConsoleTimer
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio._

final case class WhenStep(toRun: Map[Int, WhenStepRuntime[Unit]]) {
  def add(other: Map[Int, WhenStepRuntime[Unit]]): WhenStep = WhenStep(
    toRun.merge(other)
  )

  def addStepOnLastBatch(step: WhenStepRuntime[Unit]): WhenStep = {
    val currentBatch = this.toRun.keys.max
    this.copy(toRun = toRun.merge(Map(currentBatch -> step)))
  }

  def orderedBatchesToRun[V](
                              f: (Int, WhenStepRuntime[Unit]) => WhenStepRuntime[V]
                            ): WhenStepRuntime[Seq[V]] =
    ZIO.foreach(toRun.toSeq.sortBy(_._1)) { case (i, u) => f(i, u) }
}

object WhenStep {
  type WhenStepRuntime[A] = ZIO[Any, Throwable, A]

  def empty: WhenStep = WhenStep(Map(0 -> ZIO.unit))
}

trait WhenSteps {
  def registerWhenScript(whenStep: WhenStep, callScript: CallScript): WhenStep

  def registerWhen(
                    whenStep: WhenStep,
                    recordsToSend: List[(Int, List[RecordRead])]
                  ): WhenStep

  def run(
           whenStep: WhenStep,
           expectedRecords: List[KeyValueWithAliasesRecord]
         ): WhenStepRuntime[Map[String, Map[String, Seq[ConsumerRecord[String, Any]]]]]
}

object WhenSteps {
  implicit val mergeWhenStepRuntimes: Associative[WhenStepRuntime[Unit]] =
    new Associative[WhenStepRuntime[Unit]] {
      override def combine(m1: WhenStepRuntime[Unit], m2: WhenStepRuntime[Unit]): WhenStepRuntime[Unit] =
        m1 *> m2
    }
}

final case class WhenStepsLive(
                                backgroundContext: BackgroundContext,
                                recordConsumer: RecordConsumer,
                                recordProducer: RecordProducer
                              ) extends WhenSteps {

  override def registerWhen(
                             whenStep: WhenStep,
                             recordsToSend: List[(Int, List[RecordRead])]
                           ): WhenStep = whenStep.add(defineBatches(recordsToSend))

  override def registerWhenScript(whenStep: WhenStep, callScript: CallScript): WhenStep =
    whenStep.addStepOnLastBatch {
      for {
        result <- ZIO.attempt(callScript.run(backgroundContext))
        _ <- ZIO.logInfo(s"Call script result: ${result.toString}")
      } yield ()
    }

  override def run(
                    whenStep: WhenStep,
                    expectedRecords: List[KeyValueWithAliasesRecord]
                  ): WhenStepRuntime[Map[String, Map[String, Seq[ConsumerRecord[String, Any]]]]] = {
    val allKeys = expectedRecords
      .groupBy(_.batch)
      .map { case (k, keys) => (k, keys.groupBy(_.topicAlias).map { case (t, vs) => (t, vs.size) }) }

    val expectedRecordByBatch: Map[Int, List[KeyValueWithAliasesRecord]] = expectedRecords.groupBy(_.batch)
    whenStep
      .orderedBatchesToRun { case (batchNumber, productionStep) =>
        val topicsForABatch = expectedRecordByBatch.get(batchNumber).toList.flatten.map(_.topicAlias).distinct
        //launch production of records by batch, then parallelizing await for expected records by topic by batch
        ZIO.logInfo(s"Producing on batch $batchNumber") *> productionStep *> ZIO.foreachPar(topicsForABatch) { topicAlias =>
          ZIO.attempt(
            topicAlias -> allKeys.get(batchNumber)
              .map(keysForBatch => backgroundContext.consumeTopic(topicAlias, keysForBatch)(recordConsumer))
              .getOrElse(Map.empty)
          )
        }
          .map(r => r.toMap)
      }
      .map(_.reduce(_ merge _))
  }

  private def defineBatches(
                             list: List[(Int, List[RecordRead])]
                           ): Map[Int, WhenStepRuntime[Unit]] =
    list
      .foldLeft(Map.empty[Int, List[RecordRead]]) { case (acc, (batchNum, records)) =>
        acc.updated(
          batchNum,
          acc.getOrElse(batchNum, List.empty).concat(records)
        )
      }
      .map { case (batch, records) => (batch, defineTask(records)) }

  private def defineTask(recordsToSend: List[RecordRead]): WhenStepRuntime[Unit] = {
    ConsoleTimer.time(
      "runProduce", {
        ZIO
          .foreach(recordsToSend) { record =>
            backgroundContext.inputTopicConfigs.get(record.topicAlias) match {
              case Some(topicConfig) =>
                recordProducer.run(
                  record,
                  topicConfig,
                  backgroundContext.subjectConfigs.get(topicConfig.keyType),
                  backgroundContext.subjectConfigs.get(topicConfig.valueType)
                ) *> ZIO.logInfo(s"Record produced: ${record.toString}")
              case None =>
                ZIO.fail(
                  new IllegalArgumentException(
                    s"missing ${record.topicAlias} in background"
                  )
                )
            }
          }
          .unit
      }
    )
  }

}
