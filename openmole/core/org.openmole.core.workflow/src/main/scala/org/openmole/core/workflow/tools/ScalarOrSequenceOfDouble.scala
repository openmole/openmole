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

import scala.reflect.ClassTag

object Scalable {

  object Scaled {
    def toVariable(s: Scaled, values: List[Double], inputSize: Int) =
      s match {
        case Scalable.ScaledScalar(p, v)   ⇒ Variable(p, v) → values.tail
        case Scalable.ScaledSequence(p, v) ⇒ Variable(p, v) → values.drop(inputSize)
      }
  }

  sealed trait Scaled
  case class ScaledSequence[T: ClassTag](prototype: Val[Array[T]], s: Array[T]) extends Scaled
  case class ScaledScalar[T](prototype: Val[T], v: T) extends Scaled

  implicit def factorOfDoubleIsScalable[D](implicit bounded: Bounds[D, Double]) = new Scalable[Factor[D, Double]] {
    def isScalar(t: Factor[D, Double]) = true
    override def inputs(t: Factor[D, Double]) = Seq()
    override def prototype(t: Factor[D, Double]): Val[_] = t.value
    override def size(t: Factor[D, Double]): FromContext[Int] = 1

    override def scaled(s: Factor[D, Double])(values: Seq[Double]): FromContext[Scaled] = {
      val g = values.head
      assert(!g.isNaN)

      (bounded.min(s.domain) map2 bounded.max(s.domain)) { (min, max) ⇒
        val sc = g.scale(min, max)
        ScaledScalar(s.value, sc)
      }
    }
  }

  implicit def factorOfIntIsScalable[D](implicit bounded: Bounds[D, Int]) = new Scalable[Factor[D, Int]] {
    def isScalar(t: Factor[D, Int]) = true
    override def inputs(t: Factor[D, Int]) = Seq()
    override def prototype(t: Factor[D, Int]): Val[_] = t.value
    override def size(t: Factor[D, Int]): FromContext[Int] = 1

    override def scaled(s: Factor[D, Int])(values: Seq[Double]): FromContext[Scaled] = {
      val g = values.head
      assert(!g.isNaN)

      (bounded.min(s.domain) map2 bounded.max(s.domain)) { (min, max) ⇒
        val sc = g.scale(min, max + 1).toInt
        ScaledScalar(s.value, sc)
      }
    }
  }

  implicit def factorOfSequenceIsScalable[D](implicit bounded: Bounds[D, Array[Double]]) = new Scalable[Factor[D, Array[Double]]] {

    def isScalar(t: Factor[D, Array[Double]]) = false
    override def inputs(t: Factor[D, Array[Double]]) = Seq()
    override def prototype(t: Factor[D, Array[Double]]): Val[_] = t.value

    override def size(t: Factor[D, Array[Double]]): FromContext[Int] =
      (bounded.min(t.domain) map2 bounded.max(t.domain)) { case (min, max) ⇒ math.min(min.size, max.size) }

    override def scaled(t: Factor[D, Array[Double]])(values: Seq[Double]): FromContext[Scaled] = {

      def scaled =
        (bounded.min(t.domain) map2 bounded.max(t.domain)) {
          case (min, max) ⇒
            (values zip (min zip max)).map { case (g, (min, max)) ⇒ g.scale(min, max) }
        }

      scaled.map { sc ⇒ ScaledSequence(t.value, sc.toArray) }
    }

  }

}

trait Scalable[T] {
  def isScalar(t: T): Boolean
  def inputs(t: T): PrototypeSet
  def prototype(t: T): Val[_]
  def size(t: T): FromContext[Int]
  def scaled(t: T)(values: Seq[Double]): FromContext[Scalable.Scaled]
}

object ScalarOrSequenceOfDouble {

  def scaled(scales: Seq[ScalarOrSequenceOfDouble[_]], values: Seq[Double]): FromContext[List[Variable[_]]] = {
    @tailrec def scaled0(scales: List[ScalarOrSequenceOfDouble[_]], values: List[Double], acc: List[Variable[_]] = Nil)(context: ⇒ Context, rng: RandomProvider, newFile: NewFile, fileService: FileService): List[Variable[_]] =
      if (scales.isEmpty || values.isEmpty) acc.reverse
      else {
        val input = scales.head
        val (variable, tail) =
          input.scaled(values).map { Scalable.Scaled.toVariable(_, values, input.size(context)(rng, newFile, fileService)) }.from(context)(rng, newFile, fileService)

        scaled0(scales.tail, tail, variable :: acc)({ context + variable }, rng, newFile, fileService)
      }

    FromContext { p ⇒ scaled0(scales.toList, values.toList)(p.context, p.random, p.newFile, p.fileService) }
  }

  implicit def fromScalable[T: Scalable](t: T): ScalarOrSequenceOfDouble[T] = new ScalarOrSequenceOfDouble(t, implicitly[Scalable[T]])
}

class ScalarOrSequenceOfDouble[T](t: T, scalable: Scalable[T]) {
  def isScalar = scalable.isScalar(t)
  def inputs = scalable.inputs(t)
  def prototype = scalable.prototype(t)
  def size = scalable.size(t)
  def scaled(values: Seq[Double]) = scalable.scaled(t)(values)
}
