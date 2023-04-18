package com.lectra.kapoeira.domain.functions

trait FunctionRepository {
  def functions: Map[String, Func]

  def unapply(identifier: String): Option[Func] = {
    functions.get(identifier)
  }
}
