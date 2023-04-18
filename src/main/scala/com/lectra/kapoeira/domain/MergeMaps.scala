package com.lectra.kapoeira.domain

object MergeMaps {

  trait Associative[T] {
    def combine(m1: T, m2: T): T
  }

  implicit def associativeSeq[T]: Associative[Seq[T]] = new Associative[Seq[T]] {
    override def combine(m1: Seq[T], m2: Seq[T]) = m1.concat(m2)
  }

  implicit def associativeList[T]: Associative[List[T]] = new Associative[List[T]] {
    override def combine(m1: List[T], m2: List[T]): List[T] = m1.concat(m2)
  }

  implicit def associativeMap[K, V: Associative]: Associative[Map[K, V]] = new Associative[Map[K, V]] {
    val associativeV = implicitly[Associative[V]]
    override def combine(m1: Map[K, V], m2: Map[K, V]) =
      m2.foldLeft(m1) { case (acc, (k, v)) =>
        acc.updated(k, acc.get(k).map(o=>associativeV.combine(o, v)).getOrElse(v))
      }
  }

  implicit class AssociativeMergeMapsOps[T: Associative](associative: T) {
    def merge(other: T): T =
      implicitly[Associative[T]].combine(associative, other)
  }
}
