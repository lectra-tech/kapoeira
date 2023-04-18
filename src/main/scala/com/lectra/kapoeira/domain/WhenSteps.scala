package com.lectra.kapoeira.domain

import com.lectra.kapoeira.domain.MergeMaps._
import com.lectra.kapoeira.domain.Services.{RecordConsumer, RecordProducer}
import com.lectra.kapoeira.domain.WhenSteps._
import com.lectra.kapoeira.glue.ConsoleTimer
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio._

final case class WhenStep(toRun: Map[Int, Task[Unit]]) {
  def add(other: Map[Int, Task[Unit]]): WhenStep = WhenStep(
    toRun.merge(other)
  )

  def orderedBatchesToRun[V](
      f: (Int, Task[Unit]) => Task[V]
  ): Task[Seq[V]] =
    ZIO.foreach(toRun.toSeq.sortBy(_._1)) { case (i, u) => f(i, u) }
}
object WhenStep {
  def empty: WhenStep = WhenStep(Map(0 -> ZIO.unit))
}
trait WhenSteps {
  def registerWhen(
      whenStep: WhenStep,
      recordsToSend: List[(Int, List[RecordRead])]
  ): WhenStep
  def run(
      whenStep: WhenStep,
      expectedRecords: List[KeyValueWithAliasesRecord]
  ): Task[Map[String, Map[String, Seq[ConsumerRecord[String, Any]]]]]
}
object WhenSteps {
  implicit val mergeTasks: Associative[Task[Unit]] =
    new Associative[Task[Unit]] {
      override def combine(m1: Task[Unit], m2: Task[Unit]): Task[Unit] =
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

  override def run(
      whenStep: WhenStep,
      expectedRecords: List[KeyValueWithAliasesRecord]
  ): Task[Map[String, Map[String, Seq[ConsumerRecord[String, Any]]]]] = {
    val allKeys = expectedRecords
      .groupBy(_.batch)
      .map { case (k, keys) => (k, keys.groupBy(_.topicAlias).map { case (t, vs) => (t, vs.size) }) }

    val expectedRecordByBatch: Map[Int, List[KeyValueWithAliasesRecord]] = expectedRecords.groupBy(_.batch)
    whenStep
      .orderedBatchesToRun { case (batchNumber, toRun) =>
        toRun *>
          ZIO
            .foreachPar(
              expectedRecordByBatch
                .get(batchNumber)
                .toList
                .flatten
                .map(_.topicAlias)
                .distinct
            ) { topicAlias =>
              ZIO.effect(
                topicAlias ->
                  allKeys
                    .get(batchNumber)
                    .map(keysForBatch =>
                      backgroundContext
                        .consumeTopic(topicAlias, keysForBatch)(recordConsumer)
                    )
                    .getOrElse(Map.empty)
              )
            }
            .map(r => r.toMap)
      }
      .map(_.reduce(_ merge _))
  }
  private def defineBatches(
      list: List[(Int, List[RecordRead])]
  ): Map[Int, Task[Unit]] =
    list
      .foldLeft(Map.empty[Int, List[RecordRead]]) { case (acc, (batchNum, records)) =>
        acc.updated(
          batchNum,
          acc.getOrElse(batchNum, List.empty).concat(records)
        )
      }
      .map { case (batch, records) => (batch, defineTask(records)) }

  private def defineTask(recordsToSend: List[RecordRead]): Task[Unit] = {
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
                )
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
