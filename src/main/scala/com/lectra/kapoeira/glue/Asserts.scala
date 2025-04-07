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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{JsonNodeType, TextNode}
import com.lectra.kapoeira.domain.AssertionContext
import com.lectra.kapoeira.domain.AssertionContext.{HeadersValue, RecordValue}
import com.lectra.kapoeira.exception.AssertException
import com.typesafe.scalalogging.LazyLogging
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils
import io.gatling.jsonpath.{JPError, JsonPath}
import org.scalatest.Assertion
import org.scalatest.Assertions._
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._
import scala.util.Try

object Asserts extends Matchers with LazyLogging {
  def equal(
      assertionContext: AssertionContext,
      alias: String,
      jsonExpression: String,
      expected: String
  ): Assertion = assertKafkaOutput(
    assertionContext,
    alias,
    jsonExpression,
    { actual => assert(actual == JsonExpr(expected).value) }
  )

  def matchObject(
      assertionContext: AssertionContext,
      alias: String,
      jsonExpression: String,
      expected: String
  ): Assertion =
    assertKafkaOutput(
      assertionContext,
      alias,
      jsonExpression,
      { actual =>
        val actualSet = actual.toMap.toSet
        val expectedSet = JsonExpr(expected).value.toMap.toSet
        assert(expectedSet.intersect(actualSet) == expectedSet,s"Actual json $actual does not match $expected")
      }
    )

  def approxEqual(assertionContext: AssertionContext, alias: String, jsonExpression: String, expected: String, approxRange: String):Assertion =
    assertKafkaOutput(
    assertionContext,
    alias,
    jsonExpression,
    { actual => (
        for{
          x <- actual.double()
          expected <- JsonExpr(expected).value.double()
          approx <- JsonExpr(approxRange).value.double()
        } yield{ x should equal ( expected  +- approx ) }
      ).fold(fail(s"${actual} equal ${expected} +- ${approxRange}"))(identity)
    }
  )

  def matchExactObject(
                   assertionContext: AssertionContext,
                   alias: String,
                   jsonExpression: String,
                   expected: String
                 ): Assertion =
    assertKafkaOutput(
      assertionContext,
      alias,
      jsonExpression,
      { actual =>
        val actualSet = actual.toMap.toSet
        val expectedSet = JsonExpr(expected).value.toMap.toSet
        assert(actualSet == expectedSet, s"Actual json $actual does not match $expected")
      }
    )

  def assertKafkaOutput(
      assertionContext: AssertionContext,
      alias: String,
      jsonExpression: String,
      assertion: JsonNode => Assertion
  ): Assertion = {

    assertionContext.extractConsumedRecordWithAlias(alias) match {
      case Some(RecordValue(consumed)) =>
        val eventualJson = consumed match {
          case bytes: Array[Byte] => new String(bytes).trim
          case aString: String    => aString
          case value              => value.toString.trim
        }
        val stringValue =
          if (eventualJson.startsWith("{") || eventualJson.startsWith("["))
            eventualJson
          else new String(AvroSchemaUtils.toJson(consumed))
        assertJson(jsonExpression, assertion, stringValue)
      case Some(HeadersValue(headers)) =>
        Try { headers.map { case (k, v) => (k , new String(v)) } }
          .flatMap(hm => Try { objectMapper.valueToTree[JsonNode](hm.asJava) })
          .map(jsonValue => assertJson(jsonExpression, assertion, jsonValue))
          .fold(
            err =>
              throw AssertException(
                s"Error when assert on $alias : ${err.getMessage}"
              ),
            identity
          )
      case _ =>
        throw AssertException(s"Alias $alias not found in assertion context.")
    }

  }

  def assertJson(
      jsonExpression: String,
      assertion: JsonNode => Assertion,
      actual: String
  ): Assertion = {
    val jsonActual = Try {
      objectMapper.readTree(actual)
    }
      .fold(_ => new TextNode(actual), identity)
    assertJson(jsonExpression, assertion, jsonActual)
  }

  def assertJson(
      jsonExpression: String,
      assertion: JsonNode => Assertion,
      jsonActual: JsonNode
  ): Assertion = {
    logger.info(s"""jsonActual: [$jsonActual]""")
    val jsonValue: Either[JPError, Iterator[JsonNode]] =
      JsonPath.query(jsonExpression, jsonActual)
    jsonValue match {
      case Right(value) =>
        val found =
          value.next()
        logger.info(s"""jsonFound: [$found]""")
        assertion(found)
      case Left(x) =>
        logger.error("stringValue not found");
        throw new IllegalStateException(x.toString)
    }
  }

  implicit class JsonNodeOps(val jsonNode: JsonNode) {

    def double():Option[Double] ={
      val none = (_:Any) => Option.empty[Double]
      jsonNode.fold(obj = none, arr = none, number = Some(_), strng = none, bln = none, bin = none, nullOrUndef = none)
    }

    def fold[T](
        obj: JsonNode => T,
        arr: JsonNode => T,
        number: Double => T,
        strng: String => T,
        bln: Boolean => T,
        bin: Array[Byte] => T,
        nullOrUndef: JsonNode => T
    ): T = jsonNode.getNodeType match {
      case JsonNodeType.ARRAY                       => arr(jsonNode)
      case JsonNodeType.NUMBER                      => number(jsonNode.doubleValue())
      case JsonNodeType.STRING                      => strng(jsonNode.asText())
      case JsonNodeType.BOOLEAN                     => bln(jsonNode.asBoolean())
      case JsonNodeType.BOOLEAN                     => bln(jsonNode.asBoolean())
      case JsonNodeType.BINARY                      => bin(jsonNode.binaryValue())
      case JsonNodeType.MISSING | JsonNodeType.NULL => nullOrUndef(jsonNode)
      case JsonNodeType.OBJECT | JsonNodeType.POJO  => obj(jsonNode)
    }

    def recFold: Any = jsonNode.fold(
      obj =>
        obj
          .fields()
          .asScala
          .map(entry => (entry.getKey, entry.getValue.recFold))
          .toMap,
      arr => arr.elements().asScala.map(_.recFold).toSeq,
      identity,
      identity,
      identity,
      identity,
      identity
    )

    def toMap: Map[String, Any] = {
      assert(
        jsonNode.isObject,
        s"Trying to convert ${jsonNode} to Map[String,Any]"
      )
      recFold.asInstanceOf[Map[String, Any]]
    }

  }

  final case class JsonExpr(json: String) {
    def value: JsonNode =
      Try { objectMapper.readTree(json) }
        .getOrElse(new TextNode(json))
  }

}
