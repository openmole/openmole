package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.tools.ContextAggregator
import org.openmole.plugin.method.evolution.Objective.ToObjective
import org.openmole.tool.types.ToDouble

import scala.reflect.ClassTag

object Objective {

  object ToObjective {
    implicit def valIsToExact[T](implicit td: ToDouble[T]): ToObjective[Val[T]] = v ⇒ Objective[T](v, td.apply _, negative = false, delta = None, as = None)
    implicit def negativeToExact[T](implicit exact: ToObjective[T]): ToObjective[Negative[T]] = t ⇒ exact.apply(t.value).copy(negative = true)
    implicit def deltaIsToExact[T, V](implicit exact: ToObjective[T], td: ToDouble[V]): ToObjective[Delta[T, V]] = t ⇒ exact.apply(t.value).copy(delta = Some(td.apply(t.delta)))
    implicit def asIsToExact[T](implicit exact: ToObjective[T]): ToObjective[As[T, String]] = t ⇒ exact.apply(t.value).copy(as = Some(t.as))
    implicit def asValIsToExact[T, P](implicit exact: ToObjective[T]): ToObjective[As[T, Val[P]]] = t ⇒ exact.apply(t.value).copy(as = Some(t.as.name))

    implicit def aggregateStringIsNoisy[T: ClassTag]: ToObjective[Aggregate[Val[T], String]] =
      (t: Aggregate[Val[T], String]) ⇒ {
        val fromContext: FromContext[Double] = t.aggregate

        def aggregate = FromContext { p ⇒
          import p._
          (v: Array[T]) ⇒ fromContext.from(Context(t.value.toArray -> v))
        }

        Objective(t.value.array, aggregate, negative = false, delta = None, as = None, validate = fromContext.validate, noisy = true)
      }

    implicit def aggregateArrayIsToNoisy[T: ClassTag]: ToObjective[Aggregate[Val[T], Array[T] ⇒ Double]] = a ⇒ Objective(a.value.array, a.aggregate, negative = false, delta = None, as = None, noisy = true)
    implicit def aggregateSeqIsToNoisy[T: ClassTag]: ToObjective[Aggregate[Val[T], Seq[T] ⇒ Double]] = a ⇒ Objective(a.value.array, (v: Array[T]) ⇒ a.aggregate(v.toVector), negative = false, delta = None, as = None, noisy = true)
    implicit def aggregateVectorIsToNoisy[T: ClassTag]: ToObjective[Aggregate[Val[T], Vector[T] ⇒ Double]] = a ⇒ Objective(a.value.array, (v: Array[T]) ⇒ a.aggregate(v.toVector), negative = false, delta = None, as = None, noisy = true)
   }

  trait ToObjective[T] {
    def apply(t: T): Objective[_]
  }

  implicit def toObjective[T: ToObjective](t: T): Objective[_] = implicitly[ToObjective[T]].apply(t)

  def toExact(o: Objective[_]) =
    o.noisy match {
      case false ⇒ o
      case true  ⇒ throw new UserBadDataError(s"Objective $o cannot be aggregated it should be exact.")
    }

  def toNoisy[T](o: Objective[T]) = {
    o.noisy match {
      case true ⇒ o
      case false ⇒
        def medianAggregation(e: Objective[T]) = FromContext { p ⇒
          import p._
          import org.openmole.tool.statistics._
          (p: Array[T]) ⇒ p.map(e.toDouble.from(context)).median
        }

        Objective(o.prototype.array, medianAggregation(o), o.negative, o.delta, o.as, noisy = true)
    }
  }

  def toFitnessFunction(phenotypeContent: PhenotypeContent, objectives: Seq[Objective[_]]) = FromContext { p ⇒
    import p._
    (phenotype: Phenotype) ⇒ {
      val context = Phenotype.toContext(phenotypeContent, phenotype)
      objectives.toVector.map(_.value.from(context))
    }
  }

  def aggregate(phenotypeContent: PhenotypeContent, objectives: Seq[Objective[_]]) = FromContext { p ⇒
    import p._

    (v: Vector[Phenotype]) ⇒
      val aggregatedContext = ContextAggregator.aggregateSimilar(v.map(p ⇒ Phenotype.toContext(phenotypeContent, p)))
      objectives.toVector.map { _.value.from(context ++ aggregatedContext.values) }
  }

  def prototype(o: Objective[_]) = if (!o.noisy) o.prototype else o.prototype.unsecureFromArray

}

case class Objective[P](
  prototype: Val[P],
  toDouble:  FromContext[P ⇒ Double],
  negative:  Boolean,
  delta:     Option[Double],
  as:        Option[String],
  noisy:     Boolean                 = false,
  validate:  Validate                = Validate.success) {

  private def value = FromContext { p ⇒
    import p._
    val value = toDouble.from(context).apply(context(prototype))

    def deltaValue =
      delta match {
        case Some(delta) ⇒ math.abs(value - delta)
        case None        ⇒ value
      }

    if (!negative) deltaValue else -deltaValue
  }

}

object Objectives {

  def onlyExact(o: Objectives) = o.collect { case x if !x.noisy ⇒ x }.size == o.size
  def toExact(o: Objectives) = o.map(o ⇒ Objective.toExact(o))
  def toNoisy(o: Objectives) = o.map(o ⇒ Objective.toNoisy(o))

  def resultPrototypes(o: Objectives) = {
    def resultPrototype(o: Objective[_]) =
      (o.delta, o.as) match {
        case (_, Some(s))    ⇒ Objective.prototype(o).withName(s)
        case (Some(_), None) ⇒ Objective.prototype(o).withNamespace(o.prototype.namespace.names ++ Seq("delta"))
        case _               ⇒ Objective.prototype(o)
      }

    o.map(resultPrototype)
  }

  def validate(o: Objectives, outputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    o flatMap { o ⇒ o.validate(inputs ++ outputs) }
  }

  def prototypes(o: Objectives) = o.map(Objective.prototype)
}