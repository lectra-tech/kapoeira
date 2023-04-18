package com.lectra.kapoeira.glue

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReadHeadersTest extends AnyFlatSpec with Matchers {
  behavior of "read headers from string"

  it should "parse a string to a scala Map[String,Any]" in {
    val aJson =
      """
        |{"foo":"bar","baz":42,"qux":false,"buzz":{"toto":"titi"}}
        |""".stripMargin

    val actual = aJson.readHeaders
    actual should contain theSameElementsAs Map(
      "foo" -> "bar",
      "baz" -> 42,
      "qux" -> false,
      "buzz" -> Map(
        "toto" -> "titi"
      )
    )
  }
}
