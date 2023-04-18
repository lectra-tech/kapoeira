package com.lectra.kapoeira.domain.functions

import java.util.{HexFormat, UUID}
import com.typesafe.scalalogging.LazyLogging

import java.lang.Thread.sleep
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.OffsetDateTime

object DefaultFunctionRepository extends FunctionRepository with LazyLogging {

  def functions: Map[String, Func] = Map(
    "uuid" -> { _ => UUID.randomUUID().toString },
    "uppercase" -> { args => args.head.toUpperCase() },
    "lowercase" -> { args => args.head.toLowerCase() },
    "print" -> { args => logger.info(args.mkString(" ")) },
    "now" -> { _ => OffsetDateTime.now().toString },
    "sleep" -> { args => sleep(args.head.toLong) },
    "sha256" -> {  sha256 _ },
    "sha1" -> {  sha1 _ }
  )

  def sha256(args: Array[String]): String = {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val input = args.mkString(" ")
    messageDigest.update(input.getBytes(StandardCharsets.UTF_8), 0, input.length)
    HexFormat.of().formatHex(messageDigest.digest())
  }

  def sha1(args: Array[String]): String = {
    val messageDigest = MessageDigest.getInstance("SHA-1")
    val input = args.mkString(" ")
    messageDigest.update(input.getBytes(StandardCharsets.UTF_8), 0, input.length)
    HexFormat.of().formatHex(messageDigest.digest())
  }
}
