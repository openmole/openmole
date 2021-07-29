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
    implicit def valIsToExact[T](implicit td: ToDouble[T]): ToObjective[Val[T]] = v ⇒ Objective(_ ⇒ ComputeValue(v, td.apply _), negative = false, delta = None, as = None)
    implicit def negativeToExact[T](implicit exact: ToObjective[T]): ToObjective[Negative[T]] = t ⇒ exact.apply(t.value).copy(negative = true)
    implicit def deltaIsToExact[T, V](implicit exact: ToObjective[T], td: ToDouble[V]): ToObjective[Delta[T, V]] = t ⇒ exact.apply(t.value).copy(delta = Some(td.apply(t.delta)))
    implicit def asIsToExact[T](implicit exact: ToObjective[T]): ToObjective[As[T, String]] = t ⇒ exact.apply(t.value).copy(as = Some(t.as))
    implicit def asValIsToExact[T, P](implicit exact: ToObjective[T]): ToObjective[As[T, Val[P]]] = t ⇒ exact.apply(t.value).copy(as = Some(t.as.name))

    implicit def aggregateStringIsObjective[T: ClassTag]: ToObjective[Aggregate[Val[T], String]] =
      (t: Aggregate[Val[T], String]) ⇒ {
        val fromContext: FromContext[Double] = t.aggregate

        def value(noisy: Boolean) =
          if (!noisy) {
            def aggregate = FromContext { p ⇒
              import p._
              (v: T) ⇒ fromContext.from(Context(t.value -> v))
            }

            ComputeValue(t.value, aggregate, aggregateString = true)
          }
          else {
            def aggregate = FromContext { p ⇒
              import p._
              (v: Array[T]) ⇒ fromContext.from(Context(t.value.toArray -> v))
            }

            ComputeValue(t.value.array, aggregate, aggregateString = true)
          }

        Objective(
          value,
          negative = false,
          delta = None,
          as = None,
          validate = fromContext.validate)
      }

    implicit def aggregateIsToObjective[T: ClassTag]: ToObjective[Aggregate[Val[T], T ⇒ Double]] = a ⇒ {
      Objective(_ ⇒ ComputeValue(a.value, a.aggregate), negative = false, delta = None, as = None)
    }

    implicit def aggregateArrayIsToNoisy[T: ClassTag]: ToObjective[Aggregate[Val[T], Array[T] ⇒ Double]] = a ⇒ {
      Objective(_ ⇒ ComputeValue(a.value.array, a.aggregate), negative = false, delta = None, as = None, noisy = true)
    }
    implicit def aggregateSeqIsToNoisy[T: ClassTag]: ToObjective[Aggregate[Val[T], Seq[T] ⇒ Double]] = a ⇒ Objective(_ ⇒ ComputeValue(a.value.array, (v: Array[T]) ⇒ a.aggregate(v.toVector)), negative = false, delta = None, as = None, noisy = true)
    implicit def aggregateVectorIsToNoisy[T: ClassTag]: ToObjective[Aggregate[Val[T], Vector[T] ⇒ Double]] = a ⇒ Objective(_ ⇒ ComputeValue(a.value.array, (v: Array[T]) ⇒ a.aggregate(v.toVector)), negative = false, delta = None, as = None, noisy = true)
  }

  trait ToObjective[T] {
    def apply(t: T): Objective
  }

  implicit def toObjective[T: ToObjective](t: T): Objective = implicitly[ToObjective[T]].apply(t)

  def toExact(o: Objective) =
    o.noisy match {
      case false ⇒ o
      case true  ⇒ throw new UserBadDataError(s"Objective $o aggregation has been defined for a stochastic fitness function.")
    }

  def toNoisy(o: Objective) = {
    o.noisy match {
      case true ⇒ o
      case false ⇒
        if (!o.computeValue.aggregateString) {
          def medianAggregation[T](v: ComputeValue[T]) = {
            def agg = FromContext { p ⇒
              import p._
              import org.openmole.tool.statistics._
              (p: Array[T]) ⇒ p.map(v.toDouble.from(context)).median
            }

            ComputeValue(v.prototype.array, agg)
          }

          Objective(_ ⇒ medianAggregation(o.computeValue), o.negative, o.delta, o.as, noisy = true)
        }
        else o.copy(noisy = true)
    }
  }

  def toFitnessFunction(phenotypeContent: PhenotypeContent, objectives: Seq[Objective]) = FromContext { p ⇒
    import p._
    (phenotype: Phenotype) ⇒ {
      val context = Phenotype.toContext(phenotypeContent, phenotype)
      objectives.toVector.map(_.value.from(context))
    }
  }

  def aggregate(phenotypeContent: PhenotypeContent, objectives: Seq[Objective]) = FromContext { p ⇒
    import p._

    (v: Vector[Phenotype]) ⇒
      val aggregatedContext = ContextAggregator.aggregateSimilar(v.map(p ⇒ Phenotype.toContext(phenotypeContent, p)))
      objectives.toVector.map { _.value.from(context ++ aggregatedContext.values) }
  }

  def prototype(o: Objective) = if (!o.noisy) o.prototype else o.prototype.unsecureFromArray

  case class ComputeValue[P](
    prototype:       Val[P],
    toDouble:        FromContext[P ⇒ Double],
    aggregateString: Boolean                 = false) {

    def apply(delta: Option[Double], negative: Boolean) = FromContext { p ⇒
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
}

case class Objective(
  v:        Boolean ⇒ Objective.ComputeValue[_],
  negative: Boolean,
  delta:    Option[Double],
  as:       Option[String],
  noisy:    Boolean                             = false,
  validate: Validate                            = Validate.success) {

  lazy val computeValue = v(noisy)

  private def value = computeValue(delta, negative)
  private def prototype = computeValue.prototype

}

object Objectives {

  def onlyExact(o: Objectives) = o.collect { case x if !x.noisy ⇒ x }.size == o.size
  def toExact(o: Objectives) = o.map(o ⇒ Objective.toExact(o))
  def toNoisy(o: Objectives) = o.map(o ⇒ Objective.toNoisy(o))

  def resultPrototypes(o: Objectives) = {
    def resultPrototype(o: Objective) =
      (o.delta, o.as) match {
        case (_, Some(s))    ⇒ Objective.prototype(o).withName(s)
        case (Some(_), None) ⇒ Objective.prototype(o).withNamespace(Objective.prototype(o).namespace.names ++ Seq("delta"))
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