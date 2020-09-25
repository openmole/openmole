package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.exception.UserBadDataError
import org.openmole.plugin.method.evolution.Objective.ToObjective
import org.openmole.tool.types.ToDouble

import scala.reflect.ClassTag

object Objective {

  object ToExactObjective {
    implicit def valIsToExact[T](implicit td: ToDouble[T]) =
      new ToExactObjective[Val[T]] {
        override def apply(v: Val[T]) = ExactObjective[T](v, _(v), td.apply, negative = false, delta = None, as = None)
      }

    implicit def negativeToExact[T](implicit exact: ToExactObjective[T]) =
      new ToExactObjective[Negative[T]] {
        override def apply(t: Negative[T]): ExactObjective[_] = exact.apply(t.value).copy(negative = true)
      }

    implicit def deltaIsToExact[T, V](implicit exact: ToExactObjective[T], td: ToDouble[V]) =
      new ToExactObjective[Delta[T, V]] {
        override def apply(t: Delta[T, V]): ExactObjective[_] = exact.apply(t.value).copy(delta = Some(td.apply(t.delta)))
      }

    implicit def asIsToExact[T](implicit exact: ToExactObjective[T]) =
      new ToExactObjective[As[T, String]] {
        override def apply(t: As[T, String]): ExactObjective[_] = exact.apply(t.value).copy(as = Some(t.as))
      }

    implicit def asValIsToExact[T, P](implicit exact: ToExactObjective[T]) =
      new ToExactObjective[As[T, Val[P]]] {
        override def apply(t: As[T, Val[P]]): ExactObjective[_] = exact.apply(t.value).copy(as = Some(t.as.name))
      }

  }

  trait ToExactObjective[T] {
    def apply(t: T): ExactObjective[_]
  }

  object ToNoisyObjective {
    implicit def aggregateArrayIsToNoisy[T: ClassTag] =
      new ToNoisyObjective[Aggregate[Val[T], Array[T] ⇒ Double]] {
        override def apply(a: Aggregate[Val[T], Array[T] ⇒ Double]) = NoisyObjective(a.value, _(a.value), a.aggregate, negative = false, delta = None, as = None)
      }

    implicit def aggregateSetIsToNoisy[T: ClassTag] =
      new ToNoisyObjective[Aggregate[Val[T], Seq[T] ⇒ Double]] {
        override def apply(a: Aggregate[Val[T], Seq[T] ⇒ Double]) = NoisyObjective(a.value, _(a.value), (v: Array[T]) ⇒ a.aggregate(v.toVector), negative = false, delta = None, as = None)
      }

    implicit def aggregateVectorIsToNoisy[T: ClassTag] =
      new ToNoisyObjective[Aggregate[Val[T], Vector[T] ⇒ Double]] {
        override def apply(a: Aggregate[Val[T], Vector[T] ⇒ Double]) = NoisyObjective(a.value, _(a.value), (v: Array[T]) ⇒ a.aggregate(v.toVector), negative = false, delta = None, as = None)
      }

    implicit def negativeIsToNoisy[T](implicit noisy: ToNoisyObjective[T]) =
      new ToNoisyObjective[Negative[T]] {
        override def apply(t: Negative[T]): NoisyObjective[_] = noisy.apply(t.value).copy(negative = true)
      }

    implicit def deltaIsToNoisy[T, V](implicit noisy: ToNoisyObjective[T], td: ToDouble[V]) =
      new ToNoisyObjective[Delta[T, V]] {
        override def apply(t: Delta[T, V]): NoisyObjective[_] = noisy.apply(t.value).copy(delta = Some(td.apply(t.delta)))
      }

    implicit def asStringIsToNoisy[T](implicit noisy: ToNoisyObjective[T]) =
      new ToNoisyObjective[As[T, String]] {
        override def apply(t: As[T, String]): NoisyObjective[_] = noisy.apply(t.value).copy(as = Some(t.as))
      }

    implicit def asValIsToNoisy[T, P](implicit noisy: ToNoisyObjective[T]) =
      new ToNoisyObjective[As[T, Val[P]]] {
        override def apply(t: As[T, Val[P]]): NoisyObjective[_] = noisy.apply(t.value).copy(as = Some(t.as.name))
      }

  }

  trait ToNoisyObjective[T] {
    def apply(t: T): NoisyObjective[_]
  }

  object ToObjective {
    implicit def toNoisyObjective[T: ToNoisyObjective]: ToObjective[T] = new ToObjective[T] {
      def apply(t: T) = implicitly[ToNoisyObjective[T]].apply(t)
    }

    implicit def toExactObjective[T: ToExactObjective]: ToObjective[T] = new ToObjective[T] {
      def apply(t: T) = implicitly[ToExactObjective[T]].apply(t)
    }

    implicit def objectiveToObjective: ToObjective[Objective[_]] = new ToObjective[Objective[_]] {
      def apply(t: Objective[_]) = t
    }

  }

  trait ToObjective[T] {
    def apply(t: T): Objective[_]
  }

  implicit def toObjective[T: ToObjective](t: T): Objective[_] = implicitly[ToObjective[T]].apply(t)

  def prototype(o: Objective[_]) =
    o match {
      case e: ExactObjective[_] ⇒ e.prototype
      case n: NoisyObjective[_] ⇒ n.prototype
    }

  def resultPrototype(o: Objective[_]) =
    o match {
      case e: ExactObjective[_] ⇒
        e.delta match {
          case Some(_) ⇒ e.prototype.withNamespace(e.prototype.namespace.names ++ Seq("delta"))
          case _       ⇒ e.prototype
        }
      case n: NoisyObjective[_] ⇒
        (n.delta, n.as) match {
          case (_, Some(s))    ⇒ n.prototype.withName(s)
          case (Some(_), None) ⇒ n.prototype.withNamespace(n.prototype.namespace.names ++ Seq("delta"))
          case (None, None)    ⇒ n.prototype
        }

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
        NoisyObjective(e.prototype, e.get, pMedian, e.negative, e.delta, e.as)
    }

}

sealed trait Objective[P]

object Objectives {

  def onlyExact(o: Objectives) = Objectives.value(o).collect { case x: ExactObjective[_] ⇒ x }.size == Objectives.value(o).size
  def toExact(o: Objectives) = Objectives.value(o).map(o ⇒ Objective.toExact(o))
  def toNoisy(o: Objectives) = Objectives.value(o).map(o ⇒ Objective.toNoisy(o))

  def index(obj: Objectives, v: Val[_]): Option[Int] = index(Objectives.value(obj), v)

  def index(obj: Seq[Objective[_]], v: Val[_]): Option[Int] =
    obj.indexWhere(o ⇒ Objective.prototype(o) == v) match {
      case -1 ⇒ None
      case x  ⇒ Some(x)
    }

  implicit def singleToObjectives[T: ToObjective](t: T): Objectives = SingleObjective(implicitly[ToObjective[T]].apply(t))
  implicit def multipleToObjectives[T: ToObjective](t: Seq[T]): Objectives = MultipleObjectives(t.map(implicitly[ToObjective[T]].apply))

  case class SingleObjective(o: Objective[_]) extends Objectives
  case class MultipleObjectives(o: Seq[Objective[_]]) extends Objectives

  def value(o: Objectives) =
    o match {
      case s: SingleObjective    ⇒ Seq(s.o)
      case m: MultipleObjectives ⇒ m.o
    }

}

sealed trait Objectives

object ExactObjective {

  //  def toFitnessFunction(objectives: Seq[ExactObjective[_]])(phenotype: Array[Any]) =
  //    for {
  //      (o, p) ← (objectives zip phenotype).toVector
  //    } yield o.fromAny(p)

  def toFitnessFunction(phenotypeContent: PhenotypeContent, objectives: Seq[ExactObjective[_]])(phenotype: Phenotype) =
    for { (o, p) ← (objectives zip Phenotype.objective(phenotypeContent, phenotype)).toVector } yield o.fromAny(p)
}

case class ExactObjective[P](prototype: Val[P], get: Context ⇒ P, toDouble: P ⇒ Double, negative: Boolean, delta: Option[Double], as: Option[String]) extends Objective[P] {
  private def fromAny(v: Any) = {
    val value = toDouble(v.asInstanceOf[P])

    def deltaValue =
      delta match {
        case Some(delta) ⇒ math.abs(value - delta)
        case None        ⇒ value
      }

    if (!negative) deltaValue else -deltaValue
  }
}

object NoisyObjective {

  def aggregate(phenotypeContent: PhenotypeContent, objectives: Seq[NoisyObjective[_]])(v: Vector[Phenotype]): Vector[Double] =
    for {
      (vs, obj) ← v.map(p ⇒ Phenotype.objective(phenotypeContent, p)).transpose zip objectives
    } yield obj.aggregateAny(vs)

}

case class NoisyObjective[P: ClassTag] private (prototype: Val[P], get: Context ⇒ P, aggregate: Array[P] ⇒ Double, negative: Boolean, delta: Option[Double], as: Option[String]) extends Objective[P] {
  private def aggregateAny(values: Vector[Any]) = {
    def value = aggregate(values.map(_.asInstanceOf[P]).toArray)
    def deltaValue = delta.map(d ⇒ math.abs(value - d)).getOrElse(value)
    if (!negative) deltaValue else -deltaValue
  }
}