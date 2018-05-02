package org.openmole.plugin.method.evolution

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.expansion.FromContext
import org.openmole.core.tools.math._

import scala.annotation.tailrec
import scala.reflect.ClassTag

object Genome {

  sealed trait GenomeBound

  object GenomeBound {
    case class SequenceOfDouble(v: Val[Array[Double]], low: FromContext[Array[Double]], high: FromContext[Array[Double]], size: Int) extends GenomeBound
    case class ScalarDouble(v: Val[Double], low: FromContext[Double], high: FromContext[Double]) extends GenomeBound
    case class SequenceOfInt(v: Val[Array[Int]], low: FromContext[Array[Int]], high: FromContext[Array[Int]], size: Int) extends GenomeBound
    case class ScalarInt(v: Val[Int], low: FromContext[Int], high: FromContext[Int]) extends GenomeBound
    case class Enumeration[T](v: Val[T], values: Vector[T]) extends GenomeBound
    case class SequenceOfEnumeration[T](v: Val[Array[T]], values: Vector[Array[T]]) extends GenomeBound

    import org.openmole.core.workflow.domain._
    import org.openmole.core.workflow.sampling._

    implicit def factorIsScalaDouble[D](f: Factor[D, Double])(implicit bounded: Bounds[D, Double]) =
      ScalarDouble(f.prototype, bounded.min(f.domain), bounded.max(f.domain))

    implicit def factorIsScalarInt[D](f: Factor[D, Int])(implicit bounded: Bounds[D, Int]) =
      ScalarInt(f.prototype, bounded.min(f.domain), bounded.max(f.domain))

    implicit def factorIsSequenceOfDouble[D](f: Factor[D, Array[Double]])(implicit bounded: Bounds[D, Array[Double]], sized: Sized[D]) =
      SequenceOfDouble(f.prototype, bounded.min(f.domain), bounded.max(f.domain), sized(f.domain))

    implicit def factorIsSequenceOfInt[D](f: Factor[D, Array[Int]])(implicit bounded: Bounds[D, Array[Int]], sized: Sized[D]) =
      SequenceOfInt(f.prototype, bounded.min(f.domain), bounded.max(f.domain), sized(f.domain))

    implicit def factorIsIsEnumeration[D, T](f: Factor[D, T])(implicit fix: Fix[D, T]) =
      Enumeration(f.prototype, fix.apply(f.domain).toVector)

    implicit def factorIsSequenceOfEnumeration[D, T](f: Factor[D, Array[T]])(implicit fix: Fix[D, Array[T]]) =
      SequenceOfEnumeration(f.prototype, fix.apply(f.domain).toVector)

    def toVal(b: GenomeBound) = b match {
      case b: GenomeBound.ScalarDouble             ⇒ b.v
      case b: GenomeBound.ScalarInt                ⇒ b.v
      case b: GenomeBound.SequenceOfDouble         ⇒ b.v
      case b: GenomeBound.SequenceOfInt            ⇒ b.v
      case b: GenomeBound.Enumeration[_]           ⇒ b.v
      case b: GenomeBound.SequenceOfEnumeration[_] ⇒ b.v
    }

  }

  import _root_.mgo.{ C, D }
  import cats.implicits._

  def continuous(genome: Genome) = {
    val bounds = genome.toVector.collect {
      case s: GenomeBound.ScalarDouble ⇒
        (s.low map2 s.high) { case (l, h) ⇒ Vector(C(l, h)) }
      case s: GenomeBound.SequenceOfDouble ⇒
        (s.low map2 s.high) { case (low, high) ⇒ (low zip high).toVector.map { case (l, h) ⇒ C(l, h) } }
    }
    bounds.sequence.map(_.flatten)
  }

  def discrete(genome: Genome) = {
    val bounds = genome.toVector.collect {
      case s: GenomeBound.ScalarInt ⇒
        (s.low map2 s.high) { case (l, h) ⇒ Vector(D(l, h)) }
      case s: GenomeBound.SequenceOfInt ⇒
        (s.low map2 s.high) { case (low, high) ⇒ (low zip high).toVector.map { case (l, h) ⇒ D(l, h) } }
      case s: GenomeBound.Enumeration[_] ⇒
        FromContext { _ ⇒ Vector(D(0, s.values.size - 1)) }
      case s: GenomeBound.SequenceOfEnumeration[_] ⇒
        FromContext { _ ⇒ s.values.map { v ⇒ D(0, v.size - 1) } }
    }
    bounds.sequence.map(_.flatten)
  }

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

  def toVariables(genome: Genome, continuousValues: Vector[Double], discreteValue: Vector[Int], scale: Boolean) = {

    @tailrec def toVariables0(genome: List[Genome.GenomeBound], continuousValues: List[Double], discreteValues: List[Int], acc: List[FromContext[Variable[_]]]): FromContext[Vector[Variable[_]]] = {
      genome match {
        case Nil ⇒ acc.reverse.toVector.sequence
        case (h: GenomeBound.ScalarDouble) :: t ⇒
          val value =
            if (scale) (h.low map2 h.high)((low, high) ⇒ continuousValues.head.scale(low, high))
            else continuousValues.head.pure[FromContext]
          val v = value.map(value ⇒ Variable(h.v, value))
          toVariables0(t, continuousValues.tail, discreteValues, v :: acc)
        case (h: GenomeBound.SequenceOfDouble) :: t ⇒
          val value = (h.low map2 h.high) { (low, high) ⇒ (low zip high zip continuousValues).take(h.size) map { case ((l, h), v) ⇒ if (scale) v.scale(l, h) else v } }
          val v = value.map(value ⇒ Variable(h.v, value))
          toVariables0(t, continuousValues.drop(h.size), discreteValues, v :: acc)
        case (h: GenomeBound.ScalarInt) :: t ⇒
          val v = Variable(h.v, discreteValues.head)
          toVariables0(t, continuousValues, discreteValues.tail, v :: acc)
        case (h: GenomeBound.SequenceOfInt) :: t ⇒
          val value = (h.low map2 h.high) { (low, high) ⇒ (low zip high zip discreteValues).take(h.size) map { case (_, v) ⇒ v } }
          val v = value.map(value ⇒ Variable(h.v, value))
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
}
