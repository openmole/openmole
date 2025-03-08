package org.openmole.plugin.method.evolution

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.argument.FromContext
import org.openmole.core.tools.math._
import org.openmole.core.setter.ValueAssignment

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

import java.io.File
import scala.annotation.tailrec
import scala.reflect.ClassTag

import org.openmole.core.dsl.extension.*


object Genome:

  enum GenomeBound:
    case SequenceOfDouble(v: Val[Array[Double]], low: Array[Double], high: Array[Double], size: Int)
    case ScalarDouble(v: Val[Double], low: Double, high: Double)
    case SequenceOfInt(v: Val[Array[Int]], low: Array[Int], high: Array[Int], size: Int)
    case ScalarInt(v: Val[Int], low: Int, high: Int)
    case ContinuousInt(v: Val[Int], low: Int, high: Int)
    case SequenceOfContinuousInt(v: Val[Array[Int]], low: Array[Int], high: Array[Int], size: Int)
    case Enumeration[T](v: Val[T], values: Vector[T])
    case SequenceOfEnumeration[T](v: Val[Array[T]], values: Vector[Array[T]])

  trait GenomeBoundLowPriorityImplicits2:
    import GenomeBound.*
    import org.openmole.core.workflow.domain.*
    import org.openmole.core.workflow.sampling.*

    given [D, T](using fix: FixDomain[D, T]): Conversion[Factor[D, T], Enumeration[T]] = f =>
      Enumeration(f.value, fix(f.domain).domain.toVector)

  trait GenomeBoundLowPriorityImplicits extends GenomeBoundLowPriorityImplicits2:
    import GenomeBound.*
    import org.openmole.core.workflow.domain.*
    import org.openmole.core.workflow.sampling.*

    given [D, T: ClassTag](using fix: FixDomain[D, T]): Conversion[Factor[Seq[D], Array[T]], SequenceOfEnumeration[T]] = f =>
      SequenceOfEnumeration(f.value, f.domain.map(d => fix(d).domain.toArray).toVector)

  object GenomeBound extends GenomeBoundLowPriorityImplicits:
    import org.openmole.core.workflow.domain.*
    import org.openmole.core.workflow.sampling.*

    given [D](using bounded: BoundedDomain[D, Double]): Conversion[Factor[D, Double], ScalarDouble] = f =>
      val (min, max) = bounded(f.domain).domain
      ScalarDouble(f.value, min, max)

    given [D](using bounded: BoundedDomain[D, Int]): Conversion[Factor[D, Int], ScalarInt] = f =>
      val (min, max) = bounded(f.domain).domain
      ScalarInt(f.value, min, max)

    given [D](using bounded: BoundedDomain[D, Double]): Conversion[Factor[D, Int], ContinuousInt] = f =>
      val (min, max) = bounded(f.domain).domain
      ContinuousInt(f.value, min.toInt, max.toInt)

    given asd[D](using bounded: BoundedDomain[D, Double]): Conversion[Factor[Seq[D], Array[Double]], SequenceOfDouble] = f =>
      val (min, max) = f.domain.map(d => bounded(d).domain).unzip
      SequenceOfDouble(f.value, min.toArray, max.toArray, f.domain.size)

    given [D](using bounded: BoundedDomain[D, Int]): Conversion[Factor[Seq[D], Array[Int]], SequenceOfInt] = f =>
      val (min, max) = f.domain.map(d => bounded(d).domain).unzip
      SequenceOfInt(f.value, min.toArray, max.toArray, f.domain.size)

    given [D](using bounded: BoundedDomain[D, Double]): Conversion[Factor[Seq[D], Array[Int]], SequenceOfContinuousInt] = f =>
      val (min, max) = f.domain.map(d => bounded(d).domain).unzip
      SequenceOfContinuousInt(f.value, min.map(_.toInt).toArray, max.map(_.toInt).toArray, f.domain.size)

    def toVal(b: GenomeBound) =
      b match
        case b: GenomeBound.ScalarDouble             => b.v
        case b: GenomeBound.ScalarInt                => b.v
        case b: GenomeBound.ContinuousInt            => b.v
        case b: GenomeBound.SequenceOfDouble         => b.v
        case b: GenomeBound.SequenceOfInt            => b.v
        case b: GenomeBound.SequenceOfContinuousInt    => b.v
        case b: GenomeBound.Enumeration[?]           => b.v
        case b: GenomeBound.SequenceOfEnumeration[?] => b.v

    def size(b: GenomeBound) =
      b match
        case b: GenomeBound.ScalarDouble => 1
        case b: GenomeBound.ScalarInt => 1
        case b: GenomeBound.ContinuousInt => 1
        case b: GenomeBound.SequenceOfDouble => b.size
        case b: GenomeBound.SequenceOfInt => b.size
        case b: GenomeBound.SequenceOfContinuousInt => b.size
        case b: GenomeBound.Enumeration[?] => 1
        case b: GenomeBound.SequenceOfEnumeration[?] => b.values.size

  end GenomeBound

  import _root_.mgo.evolution.{ C, D }
  import cats.implicits.*

  extension (genome: Genome)
    def continuousGenome: Genome =
      genome.toVector.collect:
        case s: GenomeBound.ScalarDouble          => s
        case s: GenomeBound.SequenceOfDouble      => s
        case s: GenomeBound.ContinuousInt         => s
        case s: GenomeBound.SequenceOfContinuousInt => s

    def discreteGenome: Genome =
      genome.toVector.collect:
        case s: GenomeBound.ScalarInt => s
        case s: GenomeBound.SequenceOfInt => s
        case s: GenomeBound.Enumeration[?] => s
        case s: GenomeBound.SequenceOfEnumeration[?] => s

    def continuous: Vector[C] =
      genome.toVector.collect:
        case s: GenomeBound.ScalarDouble             => Vector(C(s.low, s.high))
        case s: GenomeBound.SequenceOfDouble         => (s.low zip s.high).toVector.map((l, h) => C(l, h))
        case s: GenomeBound.ContinuousInt            => Vector(C(s.low, s.high))
        case s: GenomeBound.SequenceOfContinuousInt    => (s.low zip s.high).toVector.map((l, h) => C(l, h))
      .flatten

    def discrete: Vector[D] =
      genome.toVector.collect:
        case s: GenomeBound.ScalarInt                => Vector(D(s.low, s.high))
        case s: GenomeBound.SequenceOfInt            => (s.low zip s.high).toVector.map((l, h) => D(l, h))
        case s: GenomeBound.Enumeration[?]           => Vector(D(0, s.values.size - 1))
        case s: GenomeBound.SequenceOfEnumeration[?] => s.values.map(v => D(0, v.size - 1))
      .flatten

    def toVals = genome.map(GenomeBound.toVal)
    def sizes = genome.map(GenomeBound.size)


  def continuousValue(genome: Genome, v: Val[?], continuous: IArray[Double]) =
    val index = Genome.continuousIndex(genome, v).get
    continuous(index)

  def continuousSequenceValue(genome: Genome, v: Val[?], size: Int, continuous: IArray[Double]) =
    val index = Genome.continuousIndex(genome, v).get
    continuous.slice(index, index + size)

  def discreteValue(genome: Genome, v: Val[?], discrete: IArray[Int]) =
    val index = Genome.discreteIndex(genome, v).get
    discrete(index)

  def discreteSequenceValue(genome: Genome, v: Val[?], size: Int, discrete: IArray[Int]) =
    val index = Genome.discreteIndex(genome, v).get
    discrete.slice(index, index + size)


  def continuousIndex(genome: Genome, v: Val[?]): Option[Int] =
    def indexOf0(l: List[GenomeBound], index: Int): Option[Int] =
      l match
        case Nil                                         => None
        case (h: GenomeBound.ScalarDouble) :: t          => if h.v == v then Some(index) else indexOf0(t, index + 1)
        case (h: GenomeBound.ContinuousInt) :: t         => if h.v == v then Some(index) else indexOf0(t, index + 1)
        case (h: GenomeBound.SequenceOfDouble) :: t      => if h.v == v then Some(index) else indexOf0(t, index + h.size)
        case (h: GenomeBound.SequenceOfContinuousInt) :: t => if h.v == v then Some(index) else indexOf0(t, index + h.size)

        case h :: t                                 => indexOf0(t, index)

    indexOf0(genome.toList, 0)

  def discreteIndex(genome: Genome, v: Val[?]): Option[Int] =
    def indexOf0(l: List[GenomeBound], index: Int): Option[Int] =
      l match
        case Nil => None
        case (h: GenomeBound.ScalarInt) :: t => if h.v == v then Some(index) else indexOf0(t, index + 1)
        case (h: GenomeBound.Enumeration[?]) :: t => if h.v == v then Some(index) else indexOf0(t, index + 1)
        case (h: GenomeBound.SequenceOfInt) :: t => if h.v == v then Some(index) else indexOf0(t, index + h.size)
        case (h: GenomeBound.SequenceOfEnumeration[?]) :: t => if h.v == v then Some(index) else indexOf0(t, index + h.values.size)
        case h :: t => indexOf0(t, index)

    indexOf0(genome.toList, 0)

  def valueOf(context: Context, v: Val[?]) =
    context.get(v.name) match
      case None => throw new UserBadDataError(s"Values $v has not been provided among $context")
      case Some(f) =>
        if (!v.accepts(f.value)) throw new UserBadDataError(s"Values ${f.value} is incompatible with genome part of type ${v}")
        else f.value

  def fromVariables(variables: Seq[Variable[?]], genome: Genome) =
    val vContext = Context() ++ variables

    @tailrec def fromVariables0(genome: List[Genome.GenomeBound], accInt: List[Int], accDouble: List[Double]): (IArray[Double], IArray[Int]) =
      genome match
        case Nil                                 => (IArray.from(accDouble.reverse), IArray.from(accInt.reverse))
        case (h: GenomeBound.ScalarDouble) :: t  => fromVariables0(t, accInt, valueOf(vContext, h.v).asInstanceOf[Double].normalize(h.low, h.high) :: accDouble)
        case (h: GenomeBound.ContinuousInt) :: t => fromVariables0(t, accInt, valueOf(vContext, h.v).asInstanceOf[Double].normalize(h.low, h.high) :: accDouble)
        case (h: GenomeBound.SequenceOfDouble) :: t =>
          val values =
            (h.low zip h.high zip valueOf(vContext, h.v).asInstanceOf[Array[Double]]).map { case ((low, high), v) =>
              v.normalize(low, high)
            }.toList

          fromVariables0(t, accInt, values ::: accDouble)
        case (h: GenomeBound.SequenceOfContinuousInt) :: t =>
          val values =
            (h.low zip h.high zip valueOf(vContext, h.v).asInstanceOf[Array[Double]]).map { case ((low, high), v) =>
              v.normalize(low, high)
            }.toList

          fromVariables0(t, accInt, values ::: accDouble)
        case (h: GenomeBound.ScalarInt) :: t     => fromVariables0(t, valueOf(vContext, h.v).asInstanceOf[Int] :: accInt, accDouble)
        case (h: GenomeBound.SequenceOfInt) :: t => fromVariables0(t, valueOf(vContext, h.v).asInstanceOf[Array[Int]].toList ::: accInt, accDouble)
        case (h: GenomeBound.Enumeration[?]) :: t =>
          val i = h.values.indexOf(valueOf(vContext, h.v))
          if (i == -1) throw new UserBadDataError(s"Value ${valueOf(vContext, h.v)} doesn't match a element of enumeration ${h.values} for input ${h.v}")
          fromVariables0(t, i :: accInt, accDouble)
        case (h: GenomeBound.SequenceOfEnumeration[?]) :: t =>
          val vs = valueOf(vContext, h.v).asInstanceOf[Array[?]]
          val is =
            (vs zip h.values).zipWithIndex.map {
              case ((v, hv), index) =>
                val i = hv.indexWhere(_ == v)
                if (i == -1) throw new UserBadDataError(s"Value ${v} doesn't match a element of enumeration ${hv} for at index $index of input ${h.v}")
                i
            }
          fromVariables0(t, is.toList ::: accInt, accDouble)

    fromVariables0(genome.toList, List(), List())

  def toVariables(genome: Genome, continuousValues: IArray[Double], discreteValue: IArray[Int], scale: Boolean = true) =

    @tailrec def toVariables0(genome: List[Genome.GenomeBound], continuousValues: List[Double], discreteValues: List[Int], acc: List[Variable[?]]): Vector[Variable[?]] =
      genome match
        case Nil => acc.reverse.toVector
        case (h: GenomeBound.ScalarDouble) :: t =>
          val value =
            if (scale) continuousValues.head.scale(h.low, h.high)
            else continuousValues.head
          val v = Variable(h.v, value)
          toVariables0(t, continuousValues.tail, discreteValues, v :: acc)
        case (h: GenomeBound.ContinuousInt) :: t =>
          val value =
            if (scale) continuousValues.head.scale(h.low, h.high)
            else continuousValues.head
          val v = Variable(h.v, math.ceil(value).toInt)
          toVariables0(t, continuousValues.tail, discreteValues, v :: acc)
        case (h: GenomeBound.SequenceOfDouble) :: t =>
          val value = (h.low zip h.high zip continuousValues).take(h.size) map { case ((l, h), v) => if (scale) v.scale(l, h) else v }
          val v = Variable(h.v, value)
          toVariables0(t, continuousValues.drop(h.size), discreteValues, v :: acc)
        case (h: GenomeBound.SequenceOfContinuousInt) :: t =>
          val value = (h.low zip h.high zip continuousValues).take(h.size) map { case ((l, h), v) => if (scale) v.scale(l, h) else v }
          val v = Variable(h.v, value.map(v => math.ceil(v).toInt))
          toVariables0(t, continuousValues.drop(h.size), discreteValues, v :: acc)
        case (h: GenomeBound.ScalarInt) :: t =>
          val v = Variable(h.v, discreteValues.head)
          toVariables0(t, continuousValues, discreteValues.tail, v :: acc)
        case (h: GenomeBound.SequenceOfInt) :: t =>
          val value = (h.low zip h.high zip discreteValues).take(h.size) map { case (_, v) => v }
          val v = Variable(h.v, value)
          toVariables0(t, continuousValues, discreteValues.drop(h.size), v :: acc)
        case (h: GenomeBound.Enumeration[?]) :: t =>
          val value = h.values(discreteValues.head)
          val v = Variable(h.v, value)
          toVariables0(t, continuousValues, discreteValues.tail, v :: acc)
        case (h: GenomeBound.SequenceOfEnumeration[?]) :: t =>
          val value = (h.values zip discreteValues).take(h.values.size) map { case (vs, i) => vs(i) }
          val v = Variable(h.v, value.toArray(h.v.fromArray.`type`.manifest))
          toVariables0(t, continuousValues, discreteValues.drop(h.values.size), v :: acc)

    toVariables0(genome.toList, continuousValues.toList, discreteValue.toList, List.empty)

  def toArrayVariable(genomeBound: GenomeBound, value: Seq[Any]) = genomeBound match
    case b: GenomeBound.ScalarDouble =>
      Variable(b.v.toArray, value.map(_.asInstanceOf[Double]).toArray[Double])
    case b: GenomeBound.ContinuousInt =>
      Variable(b.v.toArray, value.map(_.asInstanceOf[Int]).toArray[Int])
    case b: GenomeBound.ScalarInt =>
      Variable(b.v.toArray, value.map(_.asInstanceOf[Int]).toArray[Int])
    case b: GenomeBound.SequenceOfDouble =>
      Variable(b.v.toArray, value.map(_.asInstanceOf[Array[Double]]).toArray[Array[Double]])
    case b: GenomeBound.SequenceOfContinuousInt =>
      Variable(b.v.toArray, value.map(_.asInstanceOf[Array[Int]]).toArray[Array[Int]])
    case b: GenomeBound.SequenceOfInt =>
      Variable(b.v.toArray, value.map(_.asInstanceOf[Array[Int]]).toArray[Array[Int]])
    case b: GenomeBound.Enumeration[?] =>
      val array = b.v.`type`.manifest.newArray(value.size)
      value.zipWithIndex.foreach { case (v, i) => java.lang.reflect.Array.set(array, i, v) }
      Variable.unsecure(b.v.toArray, array)
    case b: GenomeBound.SequenceOfEnumeration[?] =>
      val array = b.v.`type`.manifest.newArray(value.size)
      value.zipWithIndex.foreach { case (v, i) => java.lang.reflect.Array.set(array, i, v) }
      Variable.unsecure(b.v.toArray, array)


  object ToSuggestion:

    import scala.util.boundary
    def loadFromFile(f: File, genome: Genome): SuggestedValues = boundary:
      import org.openmole.core.format.CSVFormat
      import org.openmole.core.format.OMRFormat
      import org.openmole.core.dsl._

      def toAssignment[T](v: Variable[T]) = ValueAssignment.untyped(v.prototype := v.value)

      if !f.exists
      then boundary.break(SuggestedValues.Error(UserBadDataError(s"File for loading suggestion $f does not exist")))

      if CSVFormat.isCSV(f)
      then
        val columns = genome.map(GenomeBound.toVal).map(v => v.name -> v)
        def values = CSVFormat.csvToVariables(f, columns).map(_.map(v => toAssignment(v)).toVector).toVector
        boundary.break(SuggestedValues.Values(values))

      if OMRFormat.isOMR(f)
      then
        val ctx = Context(OMRFormat.variables(f).head._2 *)
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

    private def fileSuggestion(t: File) =
      Suggestion(genome => loadFromFile(t, genome))

    given suggestionFromFile: ToSuggestion[File] with
      def apply(t: File): Suggestion = fileSuggestion(t)

    given suggestionFromString: ToSuggestion[String] with
      def apply(t: String): Suggestion = fileSuggestion(File(t))

    given fromAssignment: ToSuggestion[Seq[ValueAssignment[Any]]] with
      def apply(t: Seq[ValueAssignment[Any]]): Suggestion =
        Suggestion(genome => Seq(t.map(ValueAssignment.untyped)))

    given fromAssignmentSeq: ToSuggestion[Seq[Seq[ValueAssignment[Any]]]] with
      def apply(t: Seq[Seq[ValueAssignment[Any]]]): Suggestion =
        Suggestion(genome => t.map(_.map(ValueAssignment.untyped)))

  sealed trait ToSuggestion[T]:
    def apply(t: T): Suggestion

  inline implicit def toSuggestion[T](t: T)(implicit ts: ToSuggestion[T]): Suggestion = ts.apply(t)

  object Suggestion:
    def empty: Suggestion =
      Suggestion(g => SuggestedValues.empty)

    def apply(vs: :=[Val[?], Any]*): Seq[ValueAssignment[Any]] =
      vs.map(v => ValueAssignment.untyped(v.asInstanceOf[ValueAssignment[Any]]).assignment)

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

