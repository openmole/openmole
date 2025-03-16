package org.openmole.plugin.method.evolution

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.workflow.mole.ContextAggregator
import org.openmole.plugin.method.evolution.Objective.ToObjective
import org.openmole.tool.types.ToDouble
import org.openmole.plugin.task.scala.ScalaCode
import scala.reflect.ClassTag

object Objective:

  object ToObjective:
    given valIsToExact[T](using td: ToDouble[T]): ToObjective[Val[T]] = v => Objective(_ => ComputeValue(v, td.apply), negative = false, delta = None, as = None)
    given negativeToExact[T](using exact: ToObjective[T]): ToObjective[Negative[T]] = t => exact.apply(t.value).copy(negative = true)
    given deltaIsToExact[T, V](using exact: ToObjective[T], td: ToDouble[V]): ToObjective[Delta[T, V]] = t => exact.apply(t.value).copy(delta = Some(td.apply(t.delta)))
    given asIsToExact[T](using exact: ToObjective[T]): ToObjective[As[T, String]] = t => exact.apply(t.value).copy(as = Some(t.as))
    given asValIsToExact[T, P](using exact: ToObjective[T]): ToObjective[As[T, Val[P]]] = t => exact.apply(t.value).copy(as = Some(t.as.name))

    def buildAggregateCodeObjective[T: ClassTag](o: Val[T], fromContext: FromContext[Double]) =
      def value(noisy: Boolean) =
        if !noisy
        then
          def aggregate = FromContext { p =>
            import p._
            (v: T) => fromContext.from(Context(o -> v))
          }

          ComputeValue(o, aggregate, aggregateString = true)
        else
          def aggregate = FromContext { p =>
            import p._
            (v: Array[T]) => fromContext.from(Context(o.toArray -> v))
          }

          ComputeValue(o.array, aggregate, aggregateString = true)

      Objective(
        value,
        negative = false,
        delta = None,
        as = None,
        validate = fromContext.validate
      )

    implicit def evaluateScalaCodeIsObjective[T: ClassTag]: ToObjective[Evaluate[Val[T], ScalaCode | String]] = t =>
      val fromContext: FromContext[Double] = ScalaCode.fromContext(t.evaluate)
      buildAggregateCodeObjective(t.value, fromContext)

    implicit def evaluateIsToObjective[T: ClassTag]: ToObjective[Evaluate[Val[T], T => Double]] = a =>
      Objective(_ => ComputeValue(a.value, a.evaluate), negative = false, delta = None, as = None)

    implicit def evaluateArrayIsToNoisy[T: ClassTag]: ToObjective[Evaluate[Val[T], Array[T] => Double]] = a =>
      Objective(_ => ComputeValue(a.value.array, a.evaluate), negative = false, delta = None, as = None, noisy = true)

    implicit def evaluateSeqIsToNoisy[T: ClassTag]: ToObjective[Evaluate[Val[T], Seq[T] => Double]] = a => Objective(_ => ComputeValue(a.value.array, (v: Array[T]) => a.evaluate(v.toVector)), negative = false, delta = None, as = None, noisy = true)
    implicit def evaluateVectorIsToNoisy[T: ClassTag]: ToObjective[Evaluate[Val[T], Vector[T] => Double]] = a => Objective(_ => ComputeValue(a.value.array, (v: Array[T]) => a.evaluate(v.toVector)), negative = false, delta = None, as = None, noisy = true)

  trait ToObjective[T]:
    def apply(t: T): Objective

  implicit def toObjective[T: ToObjective](t: T): Objective = implicitly[ToObjective[T]].apply(t)

  def toExact(o: Objective) =
    o.noisy match
      case false => o
      case true  => throw new UserBadDataError(s"Objective $o aggregation has been defined for a stochastic fitness function.")

  def toNoisy(o: Objective) =
    o.noisy match
      case true => o
      case false =>
        if !o.computeValue.aggregateString
        then
          def medianAggregation[T](v: ComputeValue[T]) =
            def agg = FromContext { p =>
              import p._
              import org.openmole.tool.statistics._
              (p: Array[T]) => p.map(v.toDouble.from(context)).median
            }

            ComputeValue(v.prototype.array, agg)

          Objective(_ => medianAggregation(o.computeValue), o.negative, o.delta, o.as, noisy = true)
        else o.copy(noisy = true)


  def toFitnessFunction(phenotypeContent: PhenotypeContent, objectives: Objectives) =
    FromContext: p =>
      import p.*
      (phenotype: Phenotype) =>
        val context = Phenotype.toContext(phenotypeContent, phenotype)
        Objectives.values(objectives).from(context).toVector

  def aggregate(phenotypeContent: PhenotypeContent, objectives: Objectives) =
    FromContext: p =>
      import p.*

      (v: Vector[Phenotype]) =>
        val aggregatedContext =
          context ++ ContextAggregator.aggregateSimilar(v.map(p => Phenotype.toContext(phenotypeContent, p))).values
        Objectives.values(objectives).from(aggregatedContext).toVector

  def prototype(o: Objective) = if (!o.noisy) o.prototype else o.prototype.unsecureFromArray

  def resultPrototype(o: Objective) =
    def objectiveNamespace(p: Val[?]) = p.withNamespace(p.namespace.prefix("objective"))

    def p = (o.delta, o.as) match
      case (_, Some(s))    => Objective.prototype(o).withName(s)
      case (Some(_), None) => Objective.prototype(o).withNamespace(Objective.prototype(o).namespace.postfix("delta"))
      case _               => Objective.prototype(o)

    objectiveNamespace(p)

  case class ComputeValue[P](
    prototype:       Val[P],
    toDouble:        FromContext[P => Double],
    aggregateString: Boolean                 = false):

    def apply(delta: Option[Double], negative: Boolean) =
      FromContext: p =>
        import p._
        val value = toDouble.from(context).apply(context(prototype))

        def deltaValue =
          delta match
            case Some(delta) => math.abs(value - delta)
            case None        => value

        if !negative then deltaValue else -deltaValue


case class Objective(
  v:        Boolean => Objective.ComputeValue[?],
  negative: Boolean,
  delta:    Option[Double],
  as:       Option[String],
  noisy:    Boolean                             = false,
  validate: Validate                            = Validate.success):

  lazy val computeValue = v(noisy)

  def value = computeValue(delta, negative)
  def prototype = computeValue.prototype


object Objectives:

  def toSeq(o: Objectives): Seq[Objective] =
    o match
      case o: Objective => Seq(o)
      case o: Seq[Objective] => o

  def onlyExact(o: Objectives) = toSeq(o).collect { case x if !x.noisy => x }.size == toSeq(o).size
  def toExact(o: Objectives) = toSeq(o).map(o => Objective.toExact(o))
  def toNoisy(o: Objectives) = toSeq(o).map(o => Objective.toNoisy(o))

  def values(o: Objectives) =
    FromContext: p =>
      import p.*
      toSeq(o).map { _.value.from(context) }

  def resultPrototypes(o: Objectives) = toSeq(o).map(Objective.resultPrototype)

  def validate(o: Objectives, outputs: Seq[Val[?]]) =
    Validate: p =>
      import p._
      toSeq(o) flatMap { o => o.validate(inputs ++ outputs) }

  def prototypes(o: Objectives) = toSeq(o).map(Objective.prototype)