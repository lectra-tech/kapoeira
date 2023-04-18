package com.lectra.kapoeira.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BackgroundContextTest extends AnyFlatSpec with Matchers {
  "substitute variables" should "replace in a string all <variable> by its value" in {
    val template = "some ${foo} with ${bar}"
    val context = new BackgroundContext
    context.addVariable("foo","a value")
    context.addVariable("bar","42")
    val actual = context.substituteVariablesIn(template)

    actual shouldEqual "some a value with 42"
  }

  it should "not replace in a string not corresponding key" in {
    val template = "some ${} with ${x} finally"
    val context = new BackgroundContext
    context.addVariable("foo","a value")
    context.addVariable("bar","42")
    val actual = context.substituteVariablesIn(template)

    actual shouldEqual template
  }
}
