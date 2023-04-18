package com.lectra.kapoeira.glue

import com.lectra.kapoeira.domain.{BackgroundContext, RecordRead}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets

object InterpotaleSpec extends Properties("String") {
  val ctx = new BackgroundContext

  property("identity when context is empty") = forAll { (a: String) =>
    a.interpolate(ctx) == a
  }
}

class InterpotaleTest extends AnyFlatSpec with Matchers {
  behavior of "interpolate"
  it should "interpolate variables of a string" in {
    val ctx = new BackgroundContext
    ctx.addVariable("key1", "foo")
    ctx.addVariable("key2", "bar")
    val template = """${key1} is a ${key2}"""

    template.interpolate(ctx) shouldBe "foo is a bar"
  }

  it should "interpolate variables of a recursive Map structure" in {
    val ctx = new BackgroundContext
    ctx.addVariable("key1", "foo")
    ctx.addVariable("key2", "bar")

    val templateHeaders: Map[String, Any] = Map(
      "${key1}" -> "${key2}",
      "baz" -> Map(
        "${key1}" -> 42,
        "qux" -> List("quz", "${key2}")
      )
    )
    val recordRead = RecordRead(
      "${key1}Topic",
      "${key2}Key",
      "${key1}Value".getBytes(StandardCharsets.UTF_8),
      templateHeaders
    )

    val RecordRead(
      topic,
      key,
      value,
      headers
    ) = recordRead.interpolate(ctx)

    topic shouldBe "fooTopic"
    key shouldBe "barKey"
    new String(value) shouldBe "fooValue"
    headers shouldBe Map(
      "foo" -> "bar",
      "baz" -> Map(
        "foo" -> 42,
        "qux" -> List("quz", "bar")
      )
    )
  }
}
