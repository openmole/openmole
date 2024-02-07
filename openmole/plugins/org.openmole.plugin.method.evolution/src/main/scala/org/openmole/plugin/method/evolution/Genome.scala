package org.openmole.plugin.method.evolution

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fromcontext.FromContext
import org.openmole.core.tools.math._
import org.openmole.core.setter.ValueAssignment
import org.openmole.tool.collection.DoubleRange

import java.io.File
import scala.annotation.tailrec
import scala.reflect.ClassTag

import org.openmole.core.dsl.extension.*

object GenomeDouble {

  def toVariables(genome: GenomeDouble, continuousValues: Vector[Double], scale: Boolean = true) = {

    @tailrec def toVariables0(genome: List[Genome.GenomeBound.ScalarDouble], continuousValues: List[Double], acc: List[Variable[_]]): Vector[Variable[_]] = {
      genome match {
        case Nil ⇒ acc.reverse.toVector
        case (h: Genome.GenomeBound.ScalarDouble) :: t ⇒
          val value =
            if (scale) continuousValues.head.scale(h.low, h.high)
            else continuousValues.head
          val v = Variable(h.v, value)
          toVariables0(t, continuousValues.tail, v :: acc)
      }
    }

    toVariables0(genome.toList, continuousValues.toList, List.empty)
  }

  def toArrayVariable(genomeBound: Genome.GenomeBound.ScalarDouble, value: Seq[Any]) = genomeBound match {
    case b: Genome.GenomeBound.ScalarDouble ⇒
      Variable(b.v.toArray, value.map(_.asInstanceOf[Double]).toArray[Double])
  }

  def fromVariables(variables: Seq[Variable[_]], genome: GenomeDouble) = {
    val vContext = Context() ++ variables

    @tailrec def fromVariables0(genome: List[Genome.GenomeBound.ScalarDouble], accDouble: List[Double]): Vector[Double] =
      genome match {
        case Nil                                       ⇒ accDouble.reverse.toVector
        case (h: Genome.GenomeBound.ScalarDouble) :: t ⇒ fromVariables0(t, Genome.valueOf(vContext, h.v).asInstanceOf[Double].normalize(h.low, h.high) :: accDouble)
      }

    fromVariables0(genome.toList, List())
  }
}

object Genome:

  enum GenomeBound:
    case SequenceOfDouble(v: Val[Array[Double]], low: Array[Double], high: Array[Double], size: Int)
    case ScalarDouble(v: Val[Double], low: Double, high: Double)
    case SequenceOfInt(v: Val[Array[Int]], low: Array[Int], high: Array[Int], size: Int)
    case ScalarInt(v: Val[Int], low: Int, high: Int)
    case ContinuousInt(v: Val[Int], low: Int, high: Int)
    case Enumeration[T](v: Val[T], values: Vector[T])
    case SequenceOfEnumeration[T](v: Val[Array[T]], values: Vector[Array[T]])

  object GenomeBound:
    import org.openmole.core.workflow.domain._
    import org.openmole.core.workflow.sampling._

    implicit def factorIsScalarDouble[D](f: Factor[D, Double])(implicit bounded: BoundedDomain[D, Double]): ScalarDouble =
      val (min, max) = bounded(f.domain).domain
      ScalarDouble(f.value, min, max)

    implicit def factorOfDoubleRangeIsScalaDouble(f: Factor[DoubleRange, Double]): ScalarDouble =
      ScalarDouble(f.value, f.domain.low, f.domain.high)

    implicit def factorIsScalarInt[D](f: Factor[D, Int])(implicit bounded: BoundedDomain[D, Int]): ScalarInt =
      val (min, max) = bounded(f.domain).domain
      ScalarInt(f.value, min, max)

    implicit def factorOfScalaRangeIsScalarInt(f: Factor[scala.Range, Int]): ScalarInt =
      ScalarInt(f.value, f.domain.min, f.domain.max)

    implicit def factorIntIsContinuousInt[D](f: Factor[D, Int])(implicit bounded: BoundedDomain[D, Double]): ContinuousInt =
      val (min, max) = bounded(f.domain).domain
      ContinuousInt(f.value, min.toInt, max.toInt)

    implicit def factorOfIntRangeIsContinuousInt(f: Factor[DoubleRange, Int]): ContinuousInt =
      ContinuousInt(f.value, f.domain.low.toInt, f.domain.high.toInt)

    implicit def factorIsSequenceOfDouble[D](f: Factor[D, Array[Double]])(implicit bounded: BoundedDomain[D, Array[Double]], sized: DomainSize[D]): SequenceOfDouble =
      val (min, max) = bounded(f.domain).domain
      SequenceOfDouble(f.value, min, max, sized(f.domain))

    implicit def factorIsSequenceOfInt[D](f: Factor[D, Array[Int]])(implicit bounded: BoundedDomain[D, Array[Int]], sized: DomainSize[D]): SequenceOfInt =
      val (min, max) = bounded(f.domain).domain
      SequenceOfInt(f.value, min, max, sized(f.domain))

    implicit def factorIsIsEnumeration[D, T](f: Factor[D, T])(implicit fix: FixDomain[D, T]): Enumeration[T] =
      Enumeration(f.value, fix(f.domain).domain.toVector)

    implicit def factorIsSequenceOfEnumeration[D, T](f: Factor[D, Array[T]])(implicit fix: FixDomain[D, Array[T]]): SequenceOfEnumeration[T] =
      SequenceOfEnumeration(f.value, fix(f.domain).domain.toVector)

    implicit def factorOfBooleanIsSequenceOfEnumeration(f: Factor[Int, Array[Boolean]]): SequenceOfEnumeration[Boolean] =
      SequenceOfEnumeration(f.value, Vector.fill(f.domain)(Array(true, false)))

    def toVal(b: GenomeBound) =
      b match
        case b: GenomeBound.ScalarDouble             ⇒ b.v
        case b: GenomeBound.ScalarInt                ⇒ b.v
        case b: GenomeBound.ContinuousInt            ⇒ b.v
        case b: GenomeBound.SequenceOfDouble         ⇒ b.v
        case b: GenomeBound.SequenceOfInt            ⇒ b.v
        case b: GenomeBound.Enumeration[_]           ⇒ b.v
        case b: GenomeBound.SequenceOfEnumeration[_] ⇒ b.v

  end GenomeBound

  import _root_.mgo.evolution.{ C, D }
  import cats.implicits._

  def continuous(genome: Genome) =
    genome.toVector.collect {
      case s: GenomeBound.ScalarDouble     ⇒ Vector(C(s.low, s.high))
      case s: GenomeBound.SequenceOfDouble ⇒ (s.low zip s.high).toVector.map { case (l, h) ⇒ C(l, h) }
      case s: GenomeBound.ContinuousInt    ⇒ Vector(C(s.low, s.high))
    }.flatten

  def discrete(genome: Genome) =
    genome.toVector.collect {
      case s: GenomeBound.ScalarInt                ⇒ Vector(D(s.low, s.high))
      case s: GenomeBound.SequenceOfInt            ⇒ (s.low zip s.high).toVector.map { case (l, h) ⇒ D(l, h) }
      case s: GenomeBound.Enumeration[_]           ⇒ Vector(D(0, s.values.size - 1))
      case s: GenomeBound.SequenceOfEnumeration[_] ⇒ s.values.map { v ⇒ D(0, v.size - 1) }
    }.flatten

  def continuousValue(genome: Genome, v: Val[_], continuous: Vector[Double]) = {
    val index = Genome.continuousIndex(genome, v).get
    continuous(index)
  }

  def continuousSequenceValue(genome: Genome, v: Val[_], size: Int, continuous: Vector[Double]) = {
    val index = Genome.continuousIndex(genome, v).get
    continuous.slice(index, index + size)
  }

  def discreteValue(genome: Genome, v: Val[_], discrete: Vector[Int]) = {
    val index = Genome.discreteIndex(genome, v).get
    discrete(index)
  }

  def discreteSequenceValue(genome: Genome, v: Val[_], size: Int, discrete: Vector[Int]) = {
    val index = Genome.discreteIndex(genome, v).get
    discrete.slice(index, index + size)
  }

  def toVals(genome: Genome) = genome.map(GenomeBound.toVal)

  def continuousIndex(genome: Genome, v: Val[_]): Option[Int] = {
    def indexOf0(l: List[GenomeBound], index: Int): Option[Int] = {
      l match {
        case Nil                                    ⇒ None
        case (h: GenomeBound.ScalarDouble) :: t     ⇒ if (h.v == v) Some(index) else indexOf0(t, index + 1)
        case (h: GenomeBound.ContinuousInt) :: t    ⇒ if (h.v == v) Some(index) else indexOf0(t, index + 1)
        case (h: GenomeBound.SequenceOfDouble) :: t ⇒ if (h.v == v) Some(index) else indexOf0(t, index + h.size)
        case h :: t                                 ⇒ indexOf0(t, index)
      }
    }
    indexOf0(genome.toList, 0)
  }

  def discreteIndex(genome: Genome, v: Val[_]): Option[Int] = {
    def indexOf0(l: List[GenomeBound], index: Int): Option[Int] = {
      l match {
        case Nil ⇒ None
        case (h: GenomeBound.ScalarInt) :: t ⇒ if (h.v == v) Some(index) else indexOf0(t, index + 1)
        case (h: GenomeBound.Enumeration[_]) :: t ⇒ if (h.v == v) Some(index) else indexOf0(t, index + 1)
        case (h: GenomeBound.SequenceOfInt) :: t ⇒ if (h.v == v) Some(index) else indexOf0(t, index + h.size)
        case (h: GenomeBound.SequenceOfEnumeration[_]) :: t ⇒ if (h.v == v) Some(index) else indexOf0(t, index + h.values.size)
        case h :: t ⇒ indexOf0(t, index)
      }
    }
    indexOf0(genome.toList, 0)
  }

  def valueOf(context: Context, v: Val[_]) =
    context.get(v.name) match {
      case None ⇒ throw new UserBadDataError(s"Values $v has not been provided among $context")
      case Some(f) ⇒
        if (!v.accepts(f.value)) throw new UserBadDataError(s"Values ${f.value} is incompatible with genome part of type ${v}")
        else f.value
    }

  def fromVariables(variables: Seq[Variable[_]], genome: Genome) = {
    val vContext = Context() ++ variables

    @tailrec def fromVariables0(genome: List[Genome.GenomeBound], accInt: List[Int], accDouble: List[Double]): (Vector[Double], Vector[Int]) =
      genome match {
        case Nil                                 ⇒ (accDouble.reverse.toVector, accInt.reverse.toVector)
        case (h: GenomeBound.ScalarDouble) :: t  ⇒ fromVariables0(t, accInt, valueOf(vContext, h.v).asInstanceOf[Double].normalize(h.low, h.high) :: accDouble)
        case (h: GenomeBound.ContinuousInt) :: t ⇒ fromVariables0(t, accInt, valueOf(vContext, h.v).asInstanceOf[Double].normalize(h.low, h.high) :: accDouble)
        case (h: GenomeBound.SequenceOfDouble) :: t ⇒
          val values = (h.low zip h.high zip valueOf(vContext, h.v).asInstanceOf[Array[Double]]).map { case ((low, high), v) ⇒ v.normalize(low, high) }.toList
          fromVariables0(t, accInt, values ::: accDouble)
        case (h: GenomeBound.ScalarInt) :: t     ⇒ fromVariables0(t, valueOf(vContext, h.v).asInstanceOf[Int] :: accInt, accDouble)
        case (h: GenomeBound.SequenceOfInt) :: t ⇒ fromVariables0(t, valueOf(vContext, h.v).asInstanceOf[Array[Int]].toList ::: accInt, accDouble)
        case (h: GenomeBound.Enumeration[_]) :: t ⇒
          val i = h.values.indexOf(valueOf(vContext, h.v))
          if (i == -1) throw new UserBadDataError(s"Value ${valueOf(vContext, h.v)} doesn't match a element of enumeration ${h.values} for input ${h.v}")
          fromVariables0(t, i :: accInt, accDouble)
        case (h: GenomeBound.SequenceOfEnumeration[_]) :: t ⇒
          val vs = valueOf(vContext, h.v).asInstanceOf[Array[_]]
          val is =
            (vs zip h.values).zipWithIndex.map {
              case ((v, hv), index) ⇒
                val i = hv.indexWhere(_ == v)
                if (i == -1) throw new UserBadDataError(s"Value ${v} doesn't match a element of enumeration ${hv} for at index $index of input ${h.v}")
                i
            }
          fromVariables0(t, is.toList ::: accInt, accDouble)
      }

    fromVariables0(genome.toList, List(), List())
  }

  def toVariables(genome: Genome, continuousValues: Vector[Double], discreteValue: Vector[Int], scale: Boolean = true) = {

    @tailrec def toVariables0(genome: List[Genome.GenomeBound], continuousValues: List[Double], discreteValues: List[Int], acc: List[Variable[_]]): Vector[Variable[_]] = {
      genome match {
        case Nil ⇒ acc.reverse.toVector
        case (h: GenomeBound.ScalarDouble) :: t ⇒
          val value =
            if (scale) continuousValues.head.scale(h.low, h.high)
            else continuousValues.head
          val v = Variable(h.v, value)
          toVariables0(t, continuousValues.tail, discreteValues, v :: acc)
        case (h: GenomeBound.ContinuousInt) :: t ⇒
          val value =
            if (scale) continuousValues.head.scale(h.low, h.high)
            else continuousValues.head
          val v = Variable(h.v, math.ceil(value).toInt)
          toVariables0(t, continuousValues.tail, discreteValues, v :: acc)
        case (h: GenomeBound.SequenceOfDouble) :: t ⇒
          val value = (h.low zip h.high zip continuousValues).take(h.size) map { case ((l, h), v) ⇒ if (scale) v.scale(l, h) else v }
          val v = Variable(h.v, value)
          toVariables0(t, continuousValues.drop(h.size), discreteValues, v :: acc)
        case (h: GenomeBound.ScalarInt) :: t ⇒
          val v = Variable(h.v, discreteValues.head)
          toVariables0(t, continuousValues, discreteValues.tail, v :: acc)
        case (h: GenomeBound.SequenceOfInt) :: t ⇒
          val value = (h.low zip h.high zip discreteValues).take(h.size) map { case (_, v) ⇒ v }
          val v = Variable(h.v, value)
          toVariables0(t, continuousValues, discreteValues.drop(h.size), v :: acc)
        case (h: GenomeBound.Enumeration[_]) :: t ⇒
          val value = h.values(discreteValues.head)
          val v = Variable(h.v, value)
          toVariables0(t, continuousValues, discreteValues.tail, v :: acc)
        case (h: GenomeBound.SequenceOfEnumeration[_]) :: t ⇒
          val value = (h.values zip discreteValues).take(h.values.size) map { case (vs, i) ⇒ vs(i) }
          val v = Variable(h.v, value.toArray(h.v.fromArray.`type`.manifest))
          toVariables0(t, continuousValues, discreteValues.drop(h.values.size), v :: acc)
      }
    }

    toVariables0(genome.toList, continuousValues.toList, discreteValue.toList, List.empty)
  }

  def toArrayVariable(genomeBound: GenomeBound, value: Seq[Any]) = genomeBound match {
    case b: GenomeBound.ScalarDouble ⇒
      Variable(b.v.toArray, value.map(_.asInstanceOf[Double]).toArray[Double])
    case b: GenomeBound.ContinuousInt ⇒
      Variable(b.v.toArray, value.map(_.asInstanceOf[Int]).toArray[Int])
    case b: GenomeBound.ScalarInt ⇒
      Variable(b.v.toArray, value.map(_.asInstanceOf[Int]).toArray[Int])
    case b: GenomeBound.SequenceOfDouble ⇒
      Variable(b.v.toArray, value.map(_.asInstanceOf[Array[Double]]).toArray[Array[Double]])
    case b: GenomeBound.SequenceOfInt ⇒
      Variable(b.v.toArray, value.map(_.asInstanceOf[Array[Int]]).toArray[Array[Int]])
    case b: GenomeBound.Enumeration[_] ⇒
      val array = b.v.`type`.manifest.newArray(value.size)
      value.zipWithIndex.foreach { case (v, i) ⇒ java.lang.reflect.Array.set(array, i, v) }
      Variable.unsecure(b.v.toArray, array)
    case b: GenomeBound.SequenceOfEnumeration[_] ⇒
      val array = b.v.`type`.manifest.newArray(value.size)
      value.zipWithIndex.foreach { case (v, i) ⇒ java.lang.reflect.Array.set(array, i, v) }
      Variable.unsecure(b.v.toArray, array)
  }

  object ToSuggestion:

    import scala.util.boundary
    def loadFromFile(f: File, genome: Genome)(using SerializerService): SuggestedValues = boundary:
      import org.openmole.core.csv.CSVFormat
      import org.openmole.core.omr.OMRFormat
      import org.openmole.core.dsl._

      def toAssignment[T](v: Variable[T]) = ValueAssignment.untyped(v.prototype := v.value)

      if !f.exists
      then boundary.break(SuggestedValues.Error(UserBadDataError(s"File for loading suggestion $f does not exist")))

      if CSVFormat.isCSV(f)
      then
        val columns = genome.map(GenomeBound.toVal).map(v ⇒ v.name -> v)
        def values = CSVFormat.csvToVariables(f, columns).map(_.map(v ⇒ toAssignment(v)).toVector).toVector
        boundary.break(SuggestedValues.Values(values))

      if OMRFormat.isOMR(f)
      then
        val ctx = Context(OMRFormat.toVariables(f).head._2: _*)
        val vals = genome.map(g => GenomeBound.toVal(g))
        val values =
          vals.map(v => ctx.variable(v.array).getOrElse:
            boundary.break(SuggestedValues.Error(new UserBadDataError(s"Genome component $v not found in omr file $f")))
          )

        def assigments =
          for
            line <- values.map(_.value).transpose
          yield
            (vals zip line).map((v, va) => toAssignment(Variable.unsecureUntyped(v, va)))

        boundary.break(SuggestedValues.Values(assigments))

      SuggestedValues.Error(new UserBadDataError(s"Unsupported file type for suggestion $f"))

    private def fileSuggestion(t: File)(using SerializerService) =
      Suggestion(genome => loadFromFile(t, genome))

    implicit def suggestionFromFile(using SerializerService): ToSuggestion[File] =
      new ToSuggestion[File]:
        override def apply(t: File): Suggestion = fileSuggestion(t)

    implicit def suggestionFromString(using SerializerService): ToSuggestion[String] =
      new ToSuggestion[String]:
        def apply(t: String): Suggestion = fileSuggestion(File(t))

    implicit def fromAssignment[T]: ToSuggestion[Seq[Seq[ValueAssignment[T]]]] =
      new ToSuggestion[Seq[Seq[ValueAssignment[T]]]]:
        override def apply(t: Seq[Seq[ValueAssignment[T]]]): Suggestion =
          Suggestion(genome ⇒ t.map(_.map(ValueAssignment.untyped)))

  sealed trait ToSuggestion[T]:
    def apply(t: T): Suggestion

  inline implicit def toSuggestion[T](t: T)(implicit ts: ToSuggestion[T]): Suggestion = ts.apply(t)

  object Suggestion:
    def empty: Suggestion =
      Suggestion(g => SuggestedValues.empty)

  case class Suggestion(f: Genome => SuggestedValues):
    def apply(x: Genome) = f(x)

  object SuggestedValues:
    def empty = Values(Seq())
    case class Values(values: Seq[Seq[ValueAssignment.Untyped]]) extends SuggestedValues
    case class Error(t: Throwable) extends SuggestedValues

    given Conversion[Seq[Seq[ValueAssignment.Untyped]], SuggestedValues] = v => Values(v)

    def values(s: SuggestedValues) =
      s match
        case Values(v) => v
        case Error(e) => throw e

    def errors(s: SuggestedValues) =
      s match
        case Error(e) => Seq(e)
        case _ => Seq()

  sealed trait SuggestedValues

