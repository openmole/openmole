/*
 * Copyright (C) 2017 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.tools

import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.fileservice._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workspace._
import org.openmole.tool.random._
import cats.implicits._
import org.openmole.core.exception.UserBadDataError

import scala.annotation.tailrec
import org.openmole.core.tools.math._

object Scalable {

  sealed trait Scaled
  case class ScaledSequence(prototype: Val[Array[Double]], s: Array[Double]) extends Scaled
  case class ScaledScalar(prototype: Val[Double], v: Double) extends Scaled

  case class ScalableNumber(prototype: Val[Double], min: FromContext[Double], max: FromContext[Double])
  object ScalableSequence {
    def apply(prototype: Val[Array[Double]], min: FromContext[Double], max: FromContext[Double], size: Int): ScalableSequence =
      ScalableSequence(prototype, Seq.fill(size)(min), Seq.fill(size)(max))
  }

  case class ScalableSequence(prototype: Val[Array[Double]], min: Seq[FromContext[Double]], max: Seq[FromContext[Double]])

  implicit def scalarIsScalable = new Scalable[ScalableNumber] {
    def isScalar(t: ScalableNumber) = true
    override def inputs(t: ScalableNumber) = Seq()
    override def prototype(t: ScalableNumber): Val[_] = t.prototype
    override def size(t: ScalableNumber): FromContext[Int] = 1

    override def unflatten(s: ScalableNumber)(sequence: Seq[Double], scale: Boolean): FromContext[Scaled] = {
      val g = sequence.head

      (s.min map2 s.max) { (min, max) ⇒
        val sc = if (scale) g.scale(min, max) else g
        ScaledScalar(s.prototype, sc)
      }
    }

    override def toVariable(t: ScalableNumber)(value: Seq[Any]): Variable[_] =
      Variable.unsecure(prototype(t).toArray, value.map(_.asInstanceOf[Double]).toArray[Double])
  }

  private def factorToScalar[D](f: Factor[D, Double])(implicit bounded: Bounds[D, Double]) =
    ScalableNumber(f.prototype, bounded.min(f.domain), bounded.max(f.domain))

  implicit def factorIsScalable[D](implicit bounded: Bounds[D, Double]) = new Scalable[Factor[D, Double]] {
    def isScalar(t: Factor[D, Double]) = scalarIsScalable.isScalar(factorToScalar(t))
    override def inputs(t: Factor[D, Double]): PrototypeSet = Seq()
    override def prototype(t: Factor[D, Double]): Val[_] = scalarIsScalable.prototype(factorToScalar(t))
    override def size(t: Factor[D, Double]) = scalarIsScalable.size(factorToScalar(t))
    override def unflatten(t: Factor[D, Double])(genomePart: Seq[Double], scale: Boolean): FromContext[Scaled] =
      scalarIsScalable.unflatten(factorToScalar(t))(genomePart, scale)
    override def toVariable(t: Factor[D, Double])(value: Seq[Any]): Variable[_] =
      scalarIsScalable.toVariable(factorToScalar(t))(value)
  }

  implicit def factorOfSequenceIsScalable[D](implicit bounded: Bounds[D, Array[Double]]) = new Scalable[Factor[D, Array[Double]]] {

    def isScalar(t: Factor[D, Array[Double]]) = false
    override def inputs(t: Factor[D, Array[Double]]) = Seq()
    override def prototype(t: Factor[D, Array[Double]]): Val[_] = t.prototype

    override def size(t: Factor[D, Array[Double]]): FromContext[Int] =
      (bounded.min(t.domain) map2 bounded.max(t.domain)) { case (min, max) ⇒ math.min(min.size, max.size) }

    override def unflatten(t: Factor[D, Array[Double]])(sequence: Seq[Double], scale: Boolean): FromContext[Scaled] = {

      def scaled =
        (bounded.min(t.domain) map2 bounded.max(t.domain)) {
          case (min, max) ⇒
            (sequence zip (min zip max)).map { case (g, (min, max)) ⇒ if (scale) g.scale(min, max) else g }
        }

      scaled.map { sc ⇒ ScaledSequence(t.prototype, sc.toArray) }
    }

    override def toVariable(t: Factor[D, Array[Double]])(value: Seq[Any]): Variable[_] =
      Variable.unsecure(prototype(t).toArray, value.map(_.asInstanceOf[Array[Double]]).toArray[Array[Double]])

  }

}

trait Scalable[T] {
  def isScalar(t: T): Boolean
  def inputs(t: T): PrototypeSet
  def prototype(t: T): Val[_]
  def size(t: T): FromContext[Int]
  def unflatten(t: T)(sequence: Seq[Double], scale: Boolean): FromContext[Scalable.Scaled]
  def toVariable(t: T)(value: Seq[Any]): Variable[_]
}

object ScalarOrSequenceOfDouble {

  def unflatten(scales: Seq[ScalarOrSequenceOfDouble[_]], values: Seq[Double], scale: Boolean = true): FromContext[List[Variable[_]]] = {
    @tailrec def unflatten0(scales: List[ScalarOrSequenceOfDouble[_]], values: List[Double], acc: List[Variable[_]] = Nil)(context: ⇒ Context, rng: RandomProvider, newFile: NewFile, fileService: FileService): List[Variable[_]] =
      if (scales.isEmpty || values.isEmpty) acc.reverse
      else {
        val input = scales.head
        val (variable, tail) =
          input.unflatten(values, scale).map {
            case Scalable.ScaledScalar(p, v)   ⇒ Variable(p, v) → values.tail
            case Scalable.ScaledSequence(p, v) ⇒ Variable(p, v) → values.drop(input.size(context)(rng, newFile, fileService))
          }.from(context)(rng, newFile, fileService)

        unflatten0(scales.tail, tail, variable :: acc)({ context + variable }, rng, newFile, fileService)
      }

    FromContext { p ⇒ unflatten0(scales.toList, values.toList)(p.context, p.random, p.newFile, p.fileService) }
  }

  def flatten(values: Seq[Any]) =
    values map {
      case x: Double        ⇒ Array(x)
      case x: Array[Double] ⇒ x
      case x                ⇒ throw new InternalError(s"Value $x of type ${x.getClass} should be of type Double or Array[Double]")
    }

  implicit def fromScalable[T: Scalable](t: T): ScalarOrSequenceOfDouble[T] = new ScalarOrSequenceOfDouble(t, implicitly[Scalable[T]])
}

class ScalarOrSequenceOfDouble[T](t: T, scalable: Scalable[T]) {
  def isScalar = scalable.isScalar(t)
  def inputs = scalable.inputs(t)
  def prototype = scalable.prototype(t)
  def size = scalable.size(t)
  def unflatten(values: Seq[Double], scale: Boolean) = scalable.unflatten(t)(values, scale)
  def toVariable(value: Seq[Any]): Variable[_] = scalable.toVariable(t)(value)
}
