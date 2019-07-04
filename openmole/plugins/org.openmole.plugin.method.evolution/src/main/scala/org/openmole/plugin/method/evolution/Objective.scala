package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.exception.UserBadDataError
import org.openmole.tool.types.ToDouble

import scala.reflect.ClassTag

object Objective {
  implicit def valToObjective[T](v: Val[T])(implicit td: ToDouble[T]) =
    ExactObjective[T](v, _(v), td.apply, negative = false)

  implicit def negativeValToObjective[T](v: Negative[Val[T]])(implicit td: ToDouble[T]) =
    ExactObjective[T](v.value, _(v.value), td.apply, negative = true)

  implicit def aggregateToObjective[T: ClassTag](a: Aggregate[Val[T], Array[T] ⇒ Double]) =
    NoisyObjective(a.value, _(a.value), a.aggregate, negative = false)

  implicit def negativeAggregateToObjective[T: ClassTag](a: Aggregate[Negative[Val[T]], Array[T] ⇒ Double]) =
    NoisyObjective(a.value.value, _(a.value.value), a.aggregate, negative = true)

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

  //  def apply[P: ClassTag](prototype: Val[P], get: Context ⇒ P, aggregate: Array[P] ⇒ Double, negative: Boolean): NoisyObjective[P] =
  //    NoisyObjective(prototype, get, (a: Vector[P]) ⇒ aggregate(a.toArray), negative)

}

case class NoisyObjective[P: ClassTag] private (prototype: Val[P], get: Context ⇒ P, aggregate: Array[P] ⇒ Double, negative: Boolean) extends Objective[P] {
  def aggregateAny(values: Vector[Any]) = {
    def value = aggregate(values.map(_.asInstanceOf[P]).toArray)
    if (!negative) value else -value
  }
}