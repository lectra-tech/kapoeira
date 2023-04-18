package com.lectra.kapoeira.domain

sealed trait Record {
  val topicAlias: String
  val separator: String = "#"
}

final case class KeyValueRecord(
    topicAlias: String,
    key: String,
    value: String,
    headers: Map[String, Any] = Map.empty,
    batch: Int = 0
) extends Record

final case class KeyValueWithAliasesRecord(
    topicAlias: String,
    key: String,
    valueAlias: String,
    headersAlias: Option[String] = None,
    batch: Int = 0
) extends Record

final case class FileKeyValueRecord(
    topicAlias: String,
    override val separator: String,
    file: String,
    batch: Int = 0
) extends Record

final case class FileValueRecord(
    topicAlias: String,
    key: String,
    file: String,
    batch: Int = 0
) extends Record

final case class FileFormattedValueRecord(
    topicAlias: String,
    key: String,
    file: String,
    batch: Int = 0
) extends Record
