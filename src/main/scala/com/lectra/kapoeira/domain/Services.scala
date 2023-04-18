package com.lectra.kapoeira.domain

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio.Task

object Services {

  trait OutputConfigFactory {
    def apply(
        outputTopicConfig: OutputTopicConfig,
        subjectConfigs: Map[String, SubjectConfig]
    ): OutputConfig
  }

  trait RecordConsumer {
    def apply(
        outputConfig: OutputConfig,
        expectedKeys: Map[String, Int]
    ): Map[String, Seq[ConsumerRecord[String, Any]]]
  }

  trait RecordProducer {
    def run(
        record: RecordRead,
        topicConfig: TopicConfig,
        keySubjectConfig: Option[SubjectConfig],
        valueSubjectConfig: Option[SubjectConfig]
    ): Task[Unit]
  }

  trait CloseConsumer {
    def apply(outputConfig: OutputConfig, adminClient: AdminClient): Unit
  }

  trait FileOpener {
    def apply(filePath: String): List[String]
  }

  trait ReadHeaders[T] {
    def readHeaders(t: T): Map[String, Any]
  }
}
