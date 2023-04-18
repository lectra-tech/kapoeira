package com.lectra.kapoeira.domain.functions

trait Func {
  def apply(args: Array[String] = Array()) : Any
}
