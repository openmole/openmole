package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.exception.UserBadDataError
import org.openmole.tool.types.ToDouble

import scala.reflect.ClassTag

object Objective {

  object ToExactObjective {
    implicit def valIsToExact[T](implicit td: ToDouble[T]) =
      new ToExactObjective[Val[T]] {
        override def apply(v: Val[T]) = ExactObjective[T](v, _(v), td.apply, negative = false)
      }

    implicit def negativeToExact[T](implicit exact: ToExactObjective[T]) =
      new ToExactObjective[Negative[T]] {
        override def apply(t: Negative[T]): ExactObjective[_] = exact.apply(t.value).copy(negative = true)
      }
  }

  trait ToExactObjective[T] {
    def apply(t: T): ExactObjective[_]
  }

  implicit def toExactObjective[T: ToExactObjective](t: T) = implicitly[ToExactObjective[T]].apply(t)

  object ToNoisyObjective {
    implicit def aggregateArrayIsToNoisy[T: ClassTag] =
      new ToNoisyObjective[Aggregate[Val[T], Array[T] ⇒ Double]] {
        override def apply(a: Aggregate[Val[T], Array[T] ⇒ Double]) = NoisyObjective(a.value, _(a.value), a.aggregate, negative = false)
      }

    implicit def aggregateVectorIsToNoisy[T: ClassTag] =
      new ToNoisyObjective[Aggregate[Val[T], Vector[T] ⇒ Double]] {
        override def apply(a: Aggregate[Val[T], Vector[T] ⇒ Double]) = NoisyObjective(a.value, _(a.value), (v: Array[T]) ⇒ a.aggregate(v.toVector), negative = false)
      }

    implicit def negativeIsAggregate[T](implicit noisy: ToNoisyObjective[T]) =
      new ToNoisyObjective[Negative[T]] {
        override def apply(t: Negative[T]): NoisyObjective[_] = noisy.apply(t.value).copy(negative = true)
      }
  }

  trait ToNoisyObjective[T] {
    def apply(t: T): NoisyObjective[_]
  }

  implicit def toNoisyObjective[T: ToNoisyObjective](t: T) = implicitly[ToNoisyObjective[T]].apply(t)

  def index(obj: Objectives, v: Val[_]) = obj.indexWhere(o ⇒ prototype(o) == v) match {
    case -1 ⇒ None
    case x  ⇒ Some(x)
  }

  def toDouble[P](o: ExactObjective[P], context: Context) = {
    val value = o.toDouble(o.get(context))
    if (!o.negative) value else -value
  }

  def prototype(o: Objective[_]) =
    o match {
      case e: ExactObjective[_] ⇒ e.prototype
      case n: NoisyObjective[_] ⇒ n.prototype
    }

  def toExact[P](o: Objective[P]) =
    o match {
      case e: ExactObjective[P] ⇒ e
      case n: NoisyObjective[P] ⇒ throw new UserBadDataError(s"Objective $n cannot be noisy it should be exact.")
    }

  def toNoisy[P: ClassTag](o: Objective[P]) =
    o match {
      case n: NoisyObjective[P] ⇒ n
      case e: ExactObjective[P] ⇒
        import org.openmole.tool.statistics._
        def pMedian = (p: Array[P]) ⇒ p.map(e.toDouble).median
        NoisyObjective(e.prototype, e.get, pMedian, e.negative)
    }

  def onlyExact(o: Seq[Objective[_]]) = o.collect { case x: ExactObjective[_] ⇒ x }.size == o.size

}

sealed trait Objective[P]
case class ExactObjective[P](prototype: Val[P], get: Context ⇒ P, toDouble: P ⇒ Double, negative: Boolean) extends Objective[P]

object NoisyObjective {

  def aggregate(objectives: Seq[NoisyObjective[_]])(v: Vector[Array[Any]]): Vector[Double] =
    for {
      (vs, obj) ← v.transpose zip objectives
    } yield obj.aggregateAny(vs)

}

case class NoisyObjective[P: ClassTag] private (prototype: Val[P], get: Context ⇒ P, aggregate: Array[P] ⇒ Double, negative: Boolean) extends Objective[P] {
  def aggregateAny(values: Vector[Any]) = {
    def value = aggregate(values.map(_.asInstanceOf[P]).toArray)
    if (!negative) value else -value
  }
}