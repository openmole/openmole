package org.openmole.plugin.method.evolution

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.FromContext
import org.openmole.core.tools.math._
import org.openmole.core.workflow.builder.ValueAssignment

import java.io.File

import scala.annotation.tailrec
import scala.reflect.ClassTag

object Genome {

  sealed trait GenomeBound

  object GenomeBound {
    case class SequenceOfDouble(v: Val[Array[Double]], low: Array[Double], high: Array[Double], size: Int) extends GenomeBound
    case class ScalarDouble(v: Val[Double], low: Double, high: Double) extends GenomeBound
    case class SequenceOfInt(v: Val[Array[Int]], low: Array[Int], high: Array[Int], size: Int) extends GenomeBound
    case class ScalarInt(v: Val[Int], low: Int, high: Int) extends GenomeBound
    case class Enumeration[T](v: Val[T], values: Vector[T]) extends GenomeBound
    case class SequenceOfEnumeration[T](v: Val[Array[T]], values: Vector[Array[T]]) extends GenomeBound

    import org.openmole.core.workflow.domain._
    import org.openmole.core.workflow.sampling._

    implicit def factorIsScalaDouble[D](f: Factor[D, Double])(implicit bounded: BoundedDomain[D, Double]) =
      ScalarDouble(f.value, bounded.min(f.domain), bounded.max(f.domain))

    implicit def factorIsScalarInt[D](f: Factor[D, Int])(implicit bounded: BoundedDomain[D, Int]) =
      ScalarInt(f.value, bounded.min(f.domain), bounded.max(f.domain))

    implicit def factorIsSequenceOfDouble[D](f: Factor[D, Array[Double]])(implicit bounded: BoundedDomain[D, Array[Double]], sized: SizedDomain[D]) =
      SequenceOfDouble(f.value, bounded.min(f.domain), bounded.max(f.domain), sized(f.domain))

    implicit def factorIsSequenceOfInt[D](f: Factor[D, Array[Int]])(implicit bounded: BoundedDomain[D, Array[Int]], sized: SizedDomain[D]) =
      SequenceOfInt(f.value, bounded.min(f.domain), bounded.max(f.domain), sized(f.domain))

    implicit def factorIsIsEnumeration[D, T](f: Factor[D, T])(implicit fix: FixDomain[D, T]) =
      Enumeration(f.value, fix.apply(f.domain).toVector)

    implicit def factorIsSequenceOfEnumeration[D, T](f: Factor[D, Array[T]])(implicit fix: FixDomain[D, Array[T]]) =
      SequenceOfEnumeration(f.value, fix.apply(f.domain).toVector)

    implicit def factorOfBooleanIsSequenceOfEnumeration(f: Factor[Int, Array[Boolean]]) =
      SequenceOfEnumeration(f.value, Vector.fill(f.domain)(Array(true, false)))

    def toVal(b: GenomeBound) = b match {
      case b: GenomeBound.ScalarDouble             ⇒ b.v
      case b: GenomeBound.ScalarInt                ⇒ b.v
      case b: GenomeBound.SequenceOfDouble         ⇒ b.v
      case b: GenomeBound.SequenceOfInt            ⇒ b.v
      case b: GenomeBound.Enumeration[_]           ⇒ b.v
      case b: GenomeBound.SequenceOfEnumeration[_] ⇒ b.v
    }

  }

  import _root_.mgo.evolution.{ C, D }
  import cats.implicits._

  def continuous(genome: Genome) =
    genome.toVector.collect {
      case s: GenomeBound.ScalarDouble     ⇒ Vector(C(s.low, s.high))
      case s: GenomeBound.SequenceOfDouble ⇒ (s.low zip s.high).toVector.map { case (l, h) ⇒ C(l, h) }
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

  def fromVariables(variables: Seq[Variable[_]], genome: Genome) = {

    val vContext = Context() ++ variables

    def valueOf(v: Val[_]) =
      vContext.get(v.name) match {
        case None ⇒ throw new UserBadDataError(s"Values $v has not been provided among $vContext")
        case Some(f) ⇒
          if (!v.accepts(f.value)) throw new UserBadDataError(s"Values ${f.value} is incompatible with genome part of type ${v}")
          else f.value
      }

    @tailrec def fromVariables0(genome: List[Genome.GenomeBound], accInt: List[Int], accDouble: List[Double]): (Vector[Double], Vector[Int]) =
      genome match {
        case Nil                                ⇒ (accDouble.reverse.toVector, accInt.reverse.toVector)
        case (h: GenomeBound.ScalarDouble) :: t ⇒ fromVariables0(t, accInt, valueOf(h.v).asInstanceOf[Double].normalize(h.low, h.high) :: accDouble)
        case (h: GenomeBound.SequenceOfDouble) :: t ⇒
          val values = (h.low zip h.high zip valueOf(h.v).asInstanceOf[Array[Double]]).map { case ((low, high), v) ⇒ v.normalize(low, high) }.toList
          fromVariables0(t, accInt, values ::: accDouble)
        case (h: GenomeBound.ScalarInt) :: t     ⇒ fromVariables0(t, valueOf(h.v).asInstanceOf[Int] :: accInt, accDouble)
        case (h: GenomeBound.SequenceOfInt) :: t ⇒ fromVariables0(t, valueOf(h.v).asInstanceOf[Array[Int]].toList ::: accInt, accDouble)
        case (h: GenomeBound.Enumeration[_]) :: t ⇒
          val i = h.values.indexOf(valueOf(h.v))
          if (i == -1) throw new UserBadDataError(s"Value ${valueOf(h.v)} doesn't match a element of enumeration ${h.values} for input ${h.v}")
          fromVariables0(t, i :: accInt, accDouble)
        case (h: GenomeBound.SequenceOfEnumeration[_]) :: t ⇒
          val vs = valueOf(h.v).asInstanceOf[Array[_]]
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
          implicit def tag = h.v.fromArray.`type`.manifest
          val value = (h.values zip discreteValues).take(h.values.size) map { case (vs, i) ⇒ vs(i) }
          val v = Variable(h.v, value.toArray)
          toVariables0(t, continuousValues, discreteValues.drop(h.values.size), v :: acc)
      }
    }

    toVariables0(genome.toList, continuousValues.toList, discreteValue.toList, List.empty)
  }

  def toArrayVariable(genomeBound: GenomeBound, value: Seq[Any]) = genomeBound match {
    case b: GenomeBound.ScalarDouble ⇒
      Variable(b.v.toArray, value.map(_.asInstanceOf[Double]).toArray[Double])
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

  object ToSuggestion {

    def loadFromFile(f: File, genome: Genome) = {
      import org.openmole.core.csv.csvToVariables
      import org.openmole.core.keyword.:=

      def toAssignment[T](v: Variable[T]): :=[Val[T], FromContext[T]] = :=(v.prototype, v.value)

      val columns = genome.map(GenomeBound.toVal).map(v ⇒ v.name -> v)
      csvToVariables(f, columns).map(_.map(v ⇒ toAssignment(v)).toVector).toVector
    }

    implicit def fromFile =
      new ToSuggestion[File] {
        override def apply(t: File): Suggestion = genome ⇒ loadFromFile(t, genome)
      }

    implicit def fromString =
      new ToSuggestion[String] {
        override def apply(t: String): Suggestion = genome ⇒ loadFromFile(new java.io.File(t), genome)
      }

    implicit def fromAssignment[T] =
      new ToSuggestion[Seq[Seq[ValueAssignment[T]]]] {
        override def apply(t: Seq[Seq[ValueAssignment[T]]]): Suggestion = genome ⇒ t
      }
  }

  sealed trait ToSuggestion[T] {
    def apply(t: T): Suggestion
  }

  implicit def toSuggestion[T](t: T)(implicit ts: ToSuggestion[T]): Suggestion = ts.apply(t)

  object Suggestion {
    def empty = (genome: Genome) ⇒ Seq()
  }

  type Suggestion = Genome ⇒ Seq[Seq[ValueAssignment[_]]]

}
