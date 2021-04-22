package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.exception.UserBadDataError
import org.openmole.plugin.method.evolution.Objective.ToObjective
import org.openmole.tool.types.ToDouble

import scala.reflect.ClassTag

object Objective {

  object ToExactObjective {
    implicit def valIsToExact[T](implicit td: ToDouble[T]): ToExactObjective[Val[T]] = (v: Val[T]) ⇒ ExactObjective[T](v, _(v), td.apply, negative = false, delta = None, as = None)
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

        NoisyObjective(t.value, _(t.value), aggregate, negative = false, delta = None, as = None, fromContext.validate)
      }

    implicit def aggregateArrayIsToNoisy[T: ClassTag]: ToNoisyObjective[Aggregate[Val[T], Array[T] ⇒ Double]] = (a: Aggregate[Val[T], Array[T] ⇒ Double]) ⇒ NoisyObjective(a.value, _(a.value), a.aggregate, negative = false, delta = None, as = None)
    implicit def aggregateSeqIsToNoisy[T: ClassTag]: ToNoisyObjective[Aggregate[Val[T], Seq[T] ⇒ Double]] = (a: Aggregate[Val[T], Seq[T] ⇒ Double]) ⇒ NoisyObjective(a.value, _(a.value), (v: Array[T]) ⇒ a.aggregate(v.toVector), negative = false, delta = None, as = None)
    implicit def aggregateVectorIsToNoisy[T: ClassTag]: ToNoisyObjective[Aggregate[Val[T], Vector[T] ⇒ Double]] = (a: Aggregate[Val[T], Vector[T] ⇒ Double]) ⇒ NoisyObjective(a.value, _(a.value), (v: Array[T]) ⇒ a.aggregate(v.toVector), negative = false, delta = None, as = None)
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
    implicit def objectiveToObjective: ToObjective[Objective[_]] = (t: Objective[_]) ⇒ t
  }

  trait ToObjective[-T] {
    def apply(t: T): Objective[_]
  }

  implicit def toObjective[T: ToObjective](t: T): Objective[_] = implicitly[ToObjective[T]].apply(t)

  def name(o: Objective[_]) = prototype(o).name

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
      case n: NoisyObjective[P] ⇒ throw new UserBadDataError(s"Objective $n cannot be aggregated it should be exact.")
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

  def toFitnessFunction(phenotypeContent: PhenotypeContent, objectives: Seq[ExactObjective[_]])(phenotype: Phenotype) =
    for { (o, p) ← (objectives zip Phenotype.objectives(phenotypeContent, phenotype)).toVector } yield o.fromAny(p)

}

case class ExactObjective[P](
  prototype: Val[P],
  get:       Context ⇒ P,
  toDouble:  P ⇒ Double,
  negative:  Boolean,
  delta:     Option[Double],
  as:        Option[String],
  validate:  Validate       = Validate.success) extends Objective[P] {

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

  def aggregate(phenotypeContent: PhenotypeContent, objectives: Seq[NoisyObjective[_]]) = FromContext { p ⇒
    import p._

    (v: Vector[Phenotype]) ⇒
      for {
        (vs, obj) ← v.map(p ⇒ Phenotype.objectives(phenotypeContent, p)).transpose zip objectives
      } yield obj.aggregateAny(vs).from(context)
  }

}

case class NoisyObjective[P: ClassTag] private (
  prototype: Val[P],
  get:       Context ⇒ P,
  aggregate: FromContext[Array[P] ⇒ Double],
  negative:  Boolean,
  delta:     Option[Double],
  as:        Option[String],
  validate:  Validate                       = Validate.success) extends Objective[P] {

  private def aggregateAny(values: Vector[Any]) = FromContext { p ⇒
    import p._

    def value = aggregate.from(context).apply(values.map(_.asInstanceOf[P]).toArray)
    def deltaValue = delta.map(d ⇒ math.abs(value - d)).getOrElse(value)
    if (!negative) deltaValue else -deltaValue
  }

}