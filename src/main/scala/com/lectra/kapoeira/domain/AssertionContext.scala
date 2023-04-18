package com.lectra.kapoeira.domain

import com.lectra.kapoeira.domain.AssertionContext.{HeadersValue, RecordExtraction, RecordValue}
import com.lectra.kapoeira.exception.AssertException
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import zio.Runtime

final class AssertionContext(
    val whenStepsLive: WhenSteps
) extends LazyLogging {

  private var expectedRecords: List[KeyValueWithAliasesRecord] = _
  private[domain] var expectedRecordByValueAlias: Map[String, KeyValueWithAliasesRecord] = _
  private[domain] var expectedRecordByHeadersAlias: Map[String, KeyValueWithAliasesRecord] = _
  private[domain] var expectedRecordsByTopicByKey: Map[String, Map[String, Seq[KeyValueWithAliasesRecord]]] = _
  private[domain] var consumedRecordsByTopicByKey: Map[String, Map[String, Seq[ConsumerRecord[String, Any]]]] = Map.empty

  private var whenSteps: WhenStep = WhenStep.empty

  def registerWhen(
      recordsToSend: List[(Int, List[RecordRead])]
  ): Unit = whenSteps = whenStepsLive
    .registerWhen(whenSteps, recordsToSend)

  def launchConsumption(
      expectedRecords: List[KeyValueWithAliasesRecord]
  ) = {
    // 1. context
    this.expectedRecords = expectedRecords
    expectedRecordByValueAlias = expectedRecords
      .map(r => (r.valueAlias -> r))
      .toMap
    expectedRecordByHeadersAlias = expectedRecords
      .flatMap(r =>
        r.headersAlias.map { h =>
          if (expectedRecordByValueAlias.contains(h))
            logger.warn(
              s"A value alias was already defined for $h ! Expect strange behaviors."
            )
          h -> r
        }
      )
      .toMap

    // 2. transform List[KeyValueRecord] into Map1[topic, Map[key, Seq[KeyValueRecord]]]
    expectedRecordsByTopicByKey = expectedRecords
      .groupBy(_.topicAlias)
      .map { case (k, keyValueRecordList) =>
        (k, keyValueRecordList.groupBy(_.key))
      }

    // 3. consume by Topic and group by key =>  Map2[topic, Map[key, Seq[ConsumerRecord]]]
    consumedRecordsByTopicByKey = Runtime.default
      .unsafeRunSync(whenStepsLive.run(whenSteps, expectedRecords))
      .fold(err => throw err.squash, identity)
  }

  def extractConsumedRecordWithAlias(
      alias: String
  ): Option[RecordExtraction] = {
    val extractedValue = expectedRecordByValueAlias
      .get(alias)
      .toRight(s"Alias $alias not found in value alias context")
      .flatMap(r => extract(r, cr => RecordValue(cr.value())))
    val extractedAlias = expectedRecordByHeadersAlias
      .get(alias)
      .toRight(s"Alias $alias not found in header alias context")
      .flatMap(r => extract(r, cr => HeadersValue.make(cr.headers())))
    ((extractedAlias, extractedValue) match {
      case (Left(errAlias), Left(errValue)) => Left(s"$errAlias\n$errValue")
      case _                                => extractedValue.orElse(extractedAlias)
    })
      .fold(err => throw new AssertException(err), Some(_))
  }

  private def extract(
      record: KeyValueWithAliasesRecord,
      f: ConsumerRecord[String, Any] => RecordExtraction
  ): Either[String, RecordExtraction] = {
    (
      expectedRecordsByTopicByKey.get(record.topicAlias),
      consumedRecordsByTopicByKey
        .get(record.topicAlias)
    ) match {
      case (Some(expectedForTopic), Some(consumedForTopic)) =>
        (
          expectedForTopic.get(record.key),
          consumedForTopic.get(record.key)
        ) match {
          case (Some(expectedForKey), Some(consumedForKey)) =>
            Right(f(consumedForKey(expectedForKey.indexOf(record))))
          case (Some(_), _) => Left(s"Expected key ${record.key} not found in ${consumedForTopic.keys}")
          case (_, _) =>
            Left(s"Aliases ${record} was not declared in dataTable.")
        }
      case (_, _) =>
        Left(
          s"Topic alias ${record.topicAlias} was not declared in background."
        )
    }
  }

  def showConsumedRecords: String =
    s"Consumed records : ${consumedRecordsByTopicByKey.toString()}"

}
object AssertionContext {
  sealed trait RecordExtraction
  final case class RecordValue(value: Any) extends RecordExtraction
  final case class HeadersValue(value: Map[String, Array[Byte]]) extends RecordExtraction
  object HeadersValue {
    def make(headers: Headers): HeadersValue = HeadersValue(
      headers.toArray.map(h => (h.key(), h.value())).toMap
    )
  }
}
