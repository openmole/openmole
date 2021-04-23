package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.tools.ContextAggregator
import org.openmole.plugin.method.evolution.Objective.ToObjective
import org.openmole.tool.types.ToDouble

import scala.reflect.ClassTag

object Objective {

  object ToExactObjective {
    implicit def valIsToExact[T](implicit td: ToDouble[T]): ToExactObjective[Val[T]] = (v: Val[T]) ⇒ ExactObjective[T](v, td.apply, negative = false, delta = None, as = None)
    implicit def negativeToExact[T](implicit exact: ToExactObjective[T]): ToExactObjective[Negative[T]] = (t: Negative[T]) ⇒ exact.apply(t.value).copy(negative = true)
    implicit def deltaIsToExact[T, V](implicit exact: ToExactObjective[T], td: ToDouble[V]): ToExactObjective[Delta[T, V]] = (t: Delta[T, V]) ⇒ exact.apply(t.value).copy(delta = Some(td.apply(t.delta)))
    implicit def asIsToExact[T](implicit exact: ToExactObjective[T]): ToExactObjective[As[T, String]] = (t: As[T, String]) ⇒ exact.apply(t.value).copy(as = Some(t.as))
    implicit def asValIsToExact[T, P](implicit exact: ToExactObjective[T]): ToExactObjective[As[T, Val[P]]] = (t: As[T, Val[P]]) ⇒ exact.apply(t.value).copy(as = Some(t.as.name))
  }

  trait ToExactObjective[T] {
    def apply(t: T): ExactObjective[_]
  }

  object ToNoisyObjective {
    implicit def aggregateStringIsNoisy[T: ClassTag]: ToNoisyObjective[Aggregate[Val[T], String]] =
      (t: Aggregate[Val[T], String]) ⇒ {
        val fromContext: FromContext[Double] = t.aggregate

        def aggregate = FromContext { p ⇒
          import p._
          (v: Array[T]) ⇒ fromContext.from(Context(t.value.toArray -> v))
        }

        NoisyObjective(t.value, aggregate, negative = false, delta = None, as = None, fromContext.validate)
      }

    implicit def aggregateArrayIsToNoisy[T: ClassTag]: ToNoisyObjective[Aggregate[Val[T], Array[T] ⇒ Double]] = (a: Aggregate[Val[T], Array[T] ⇒ Double]) ⇒ NoisyObjective(a.value, a.aggregate, negative = false, delta = None, as = None)
    implicit def aggregateSeqIsToNoisy[T: ClassTag]: ToNoisyObjective[Aggregate[Val[T], Seq[T] ⇒ Double]] = (a: Aggregate[Val[T], Seq[T] ⇒ Double]) ⇒ NoisyObjective(a.value, (v: Array[T]) ⇒ a.aggregate(v.toVector), negative = false, delta = None, as = None)
    implicit def aggregateVectorIsToNoisy[T: ClassTag]: ToNoisyObjective[Aggregate[Val[T], Vector[T] ⇒ Double]] = (a: Aggregate[Val[T], Vector[T] ⇒ Double]) ⇒ NoisyObjective(a.value, (v: Array[T]) ⇒ a.aggregate(v.toVector), negative = false, delta = None, as = None)
    implicit def negativeIsToNoisy[T](implicit noisy: ToNoisyObjective[T]): ToNoisyObjective[Negative[T]] = (t: Negative[T]) ⇒ noisy.apply(t.value).copy(negative = true)
    implicit def deltaIsToNoisy[T, V](implicit noisy: ToNoisyObjective[T], td: ToDouble[V]): ToNoisyObjective[Delta[T, V]] = (t: Delta[T, V]) ⇒ noisy.apply(t.value).copy(delta = Some(td.apply(t.delta)))
    implicit def asStringIsToNoisy[T](implicit noisy: ToNoisyObjective[T]): ToNoisyObjective[As[T, String]] = (t: As[T, String]) ⇒ noisy.apply(t.value).copy(as = Some(t.as))
    implicit def asValIsToNoisy[T, P](implicit noisy: ToNoisyObjective[T]): ToNoisyObjective[As[T, Val[P]]] = (t: As[T, Val[P]]) ⇒ noisy.apply(t.value).copy(as = Some(t.as.name))
  }

  trait ToNoisyObjective[T] {
    def apply(t: T): NoisyObjective[_]
  }

  object ToObjective {
    implicit def toNoisyObjective[T: ToNoisyObjective]: ToObjective[T] = (t: T) ⇒ implicitly[ToNoisyObjective[T]].apply(t)
    implicit def toExactObjective[T: ToExactObjective]: ToObjective[T] = (t: T) ⇒ implicitly[ToExactObjective[T]].apply(t)
    implicit def objectiveToObjective: ToObjective[Objective] = (t: Objective) ⇒ t
  }

  trait ToObjective[-T] {
    def apply(t: T): Objective
  }

  implicit def toObjective[T: ToObjective](t: T): Objective = implicitly[ToObjective[T]].apply(t)

  def name(o: Objective) = resultPrototype(o).name

  def prototype(o: Objective) =
    o match {
      case e: ExactObjective[_] ⇒ e.prototype
      case n: NoisyObjective[_] ⇒ n.prototype
    }

  def resultPrototype(o: Objective) =
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

  def toExact(o: Objective) =
    o match {
      case e: ExactObjective[_] ⇒ e
      case n: NoisyObjective[_] ⇒ throw new UserBadDataError(s"Objective $n cannot be aggregated it should be exact.")
    }

  def toNoisy(o: Objective) = {
    o match {
      case n: NoisyObjective[_] ⇒ n
      case e: ExactObjective[_] ⇒
        def medianAggregation[T](e: ExactObjective[T]) = {
          import org.openmole.tool.statistics._
          (p: Array[T]) ⇒ p.map(e.toDouble).median
        }
        NoisyObjective(e.prototype, medianAggregation(e), e.negative, e.delta, e.as)
    }
  }

}

sealed trait Objective

object Objectives {

  def onlyExact(o: Objectives) = Objectives.value(o).collect { case x: ExactObjective[_] ⇒ x }.size == Objectives.value(o).size
  def toExact(o: Objectives) = Objectives.value(o).map(o ⇒ Objective.toExact(o))
  def toNoisy(o: Objectives) = Objectives.value(o).map(o ⇒ Objective.toNoisy(o))

  def validate(o: Objectives, outputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    o flatMap {
      case e: ExactObjective[_] ⇒ e.validate(inputs ++ outputs)
      case n: NoisyObjective[_] ⇒ n.validate(inputs ++ outputs)
    }
  }

  def value(o: Objectives) = o

}

object ExactObjective {

  def toFitnessFunction(phenotypeContent: PhenotypeContent, objectives: Seq[ExactObjective[_]])(phenotype: Phenotype) = {
    val context = Phenotype.toContext(phenotypeContent, phenotype)
    objectives.toVector.map(_.value(context))
  }

}

case class ExactObjective[P](
  prototype: Val[P],
  toDouble:  P ⇒ Double,
  negative:  Boolean,
  delta:     Option[Double],
  as:        Option[String],
  validate:  Validate       = Validate.success) extends Objective {

  private def value(context: Context) = {
    val value = toDouble(context(prototype))

    def deltaValue =
      delta match {
        case Some(delta) ⇒ math.abs(value - delta)
        case None        ⇒ value
      }

    if (!negative) deltaValue else -deltaValue
  }

}

object NoisyObjective {

  def aggregate(phenotypeContent: PhenotypeContent, objectives: Seq[NoisyObjective[_]]) = FromContext { p ⇒
    import p._

    (v: Vector[Phenotype]) ⇒
      val aggregatedContext = ContextAggregator.aggregateSimilar(v.map(p ⇒ Phenotype.toContext(phenotypeContent, p)))
      objectives.toVector.map { _.value.from(context ++ aggregatedContext.values) }
  }

}

case class NoisyObjective[P] private (
  prototype: Val[P],
  aggregate: FromContext[Array[P] ⇒ Double],
  negative:  Boolean,
  delta:     Option[Double],
  as:        Option[String],
  validate:  Validate                       = Validate.success) extends Objective {

  private def value = FromContext { p ⇒
    import p._
    def value = aggregate.from(context).apply(context(prototype.toArray))
    def deltaValue = delta.map(d ⇒ math.abs(value - d)).getOrElse(value)
    if (!negative) deltaValue else -deltaValue
  }

}

//case class NoisyContextObjective private (
//  aggregate: FromContext[Context ⇒ Double],
//  negative:  Boolean,
//  delta:     Option[Double],
//  as:        String,
//  validate:  Validate                       = Validate.success) extends Objective[P] {
//
//  private def value = FromContext { p ⇒
//    import p._
//
//    def value = aggregate.from(context).apply(context)
//    def deltaValue = delta.map(d ⇒ math.abs(value - d)).getOrElse(value)
//    if (!negative) deltaValue else -deltaValue
//  }
//
//}