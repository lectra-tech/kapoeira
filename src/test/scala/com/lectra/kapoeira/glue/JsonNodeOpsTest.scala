package com.lectra.kapoeira.glue

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import Asserts.JsonNodeOps

class JsonNodeOpsTest extends AnyFlatSpec with Matchers {
  behavior of "JsonNodeOps"
  it should "convert to Map[String,Any] for any json object" in {
    val actualSimpleObject = objectMapper.readTree("""{"aString":"bar","aNumber":0.42,"aBoolean":true,"aNull":null}""")
    val expectedSimpleObject = Map("aString" -> "bar", "aNumber" -> 0.42, "aBoolean" -> true ).toSeq

    val actualNestedObject = objectMapper.readTree("""{"foo":{"bar":"baz"}}""")
    val expectedNestedObject = Map("foo" -> Map("bar"-> "baz")).toSeq

    val actualObjectWithArrays = objectMapper.readTree("""{"foo":["item1","item2"]}""")
    val expectedObjectWithArrays = Map("foo" -> Seq("item1","item2")).toSeq

    new JsonNodeOps(actualSimpleObject).toMap.toSeq.intersect(expectedSimpleObject) shouldEqual expectedSimpleObject
    new JsonNodeOps(actualNestedObject).toMap.toSeq.intersect(expectedNestedObject) shouldEqual expectedNestedObject
    new JsonNodeOps(actualObjectWithArrays).toMap.toSeq.intersect(expectedObjectWithArrays) shouldEqual expectedObjectWithArrays

  }
}
