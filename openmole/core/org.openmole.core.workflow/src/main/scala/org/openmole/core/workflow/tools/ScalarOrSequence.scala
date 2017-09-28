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
import scala.annotation.tailrec
import org.openmole.core.tools.math._

object Scalable {

  sealed trait Scaled
  case class ScaledSequence(prototype: Val[Array[Double]], s: Array[Double]) extends Scaled
  case class ScaledScalar(prototype: Val[Double], v: Double) extends Scaled

  implicit def scalarIsScalable = new Scalable[Scalar] {
    override def prototype(t: Scalar): Val[_] = t.prototype
    override def size(t: Scalar): Int = 1
    override def scaled(s: Scalar)(genomePart: Seq[Double]): FromContext[Scaled] = {
      val g = genomePart.head
      assert(!g.isNaN)

      (s.min map2 s.max) { (min, max) ⇒
        val sc = g.scale(min, max)
        ScaledScalar(s.prototype, sc)
      }
    }
    override def toVariable(t: Scalar)(value: Seq[Any]): Variable[_] =
      Variable.unsecure(prototype(t).toArray, value.map(_.asInstanceOf[Double]).toArray[Double])
  }

  implicit def sequenceIsScalable = new Scalable[Sequence] {
    override def prototype(t: Sequence): Val[_] = t.prototype
    override def size(t: Sequence): Int = math.min(t.min.size, t.max.size)
    override def scaled(s: Sequence)(values: Seq[Double]): FromContext[Scaled] = {
      def scaled =
        (values zip (s.min zip s.max)).toVector traverseU {
          case (g, (min, max)) ⇒ (min map2 max)(g.scale(_, _))
        }

      scaled.map { sc ⇒ ScaledSequence(s.prototype, sc.toArray) }
    }

    override def toVariable(t: Sequence)(value: Seq[Any]): Variable[_] =
      Variable.unsecure(prototype(t).toArray, value.map(_.asInstanceOf[Array[Double]]).toArray[Array[Double]])
  }

  def factorToScalar[D](f: Factor[D, Double])(implicit bounded: Bounds[D, Double]) =
    Scalar(f.prototype, bounded.min(f.domain), bounded.max(f.domain))

  implicit def factorIsScalable[D](implicit bounded: Bounds[D, Double]) = new Scalable[Factor[D, Double]] {
    override def prototype(t: Factor[D, Double]): Val[_] = scalarIsScalable.prototype(factorToScalar(t))
    override def size(t: Factor[D, Double]): Int = scalarIsScalable.size(factorToScalar(t))
    override def scaled(t: Factor[D, Double])(genomePart: Seq[Double]): FromContext[Scaled] =
      scalarIsScalable.scaled(factorToScalar(t))(genomePart)
    override def toVariable(t: Factor[D, Double])(value: Seq[Any]): Variable[_] =
      scalarIsScalable.toVariable(factorToScalar(t))(value)
  }

}

trait Scalable[T] {
  def prototype(t: T): Val[_]
  def size(t: T): Int
  def scaled(t: T)(values: Seq[Double]): FromContext[Scalable.Scaled]
  def toVariable(t: T)(value: Seq[Any]): Variable[_]
}

case class Scalar(prototype: Val[Double], min: FromContext[Double], max: FromContext[Double])

object Sequence {
  def apply(prototype: Val[Array[Double]], min: FromContext[Double], max: FromContext[Double], size: Int): Sequence =
    Sequence(prototype, Seq.fill(size)(min), Seq.fill(size)(max))
}

case class Sequence(prototype: Val[Array[Double]], min: Seq[FromContext[Double]], max: Seq[FromContext[Double]])

object ScalarOrSequence {
  @tailrec def scaled(scales: List[ScalarOrSequence[_]], values: List[Double], acc: List[Variable[_]] = Nil)(context: ⇒ Context, rng: RandomProvider, newFile: NewFile, fileService: FileService): List[Variable[_]] =
    if (scales.isEmpty || values.isEmpty) acc.reverse
    else {
      val input = scales.head
      val (variable, tail) =
        input.scaled(values).map {
          case Scalable.ScaledScalar(p, v)   ⇒ Variable(p, v) → values.tail
          case Scalable.ScaledSequence(p, v) ⇒ Variable(p, v) → values.drop(input.size)
        }.from(context)(rng, newFile, fileService)

      scaled(scales.tail, tail, variable :: acc)({ context + variable }, rng, newFile, fileService)
    }

  implicit def fromScalable[T: Scalable](t: T): ScalarOrSequence[T] = new ScalarOrSequence(t, implicitly[Scalable[T]])
}

class ScalarOrSequence[T](t: T, scalable: Scalable[T]) {
  def prototype = scalable.prototype(t)
  def size = scalable.size(t)
  def scaled(genomePart: Seq[Double]) = scalable.scaled(t)(genomePart)
  def toVariable(value: Seq[Any]): Variable[_] = scalable.toVariable(t)(value)
}
