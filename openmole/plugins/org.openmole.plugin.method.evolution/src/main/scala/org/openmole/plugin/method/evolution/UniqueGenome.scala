/*
 * Copyright (C) 27/01/14 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo.double2Scalable
import org.openmole.core.workflow.data.{ RandomProvider, Context, Prototype, Variable }
import org.openmole.core.workflow.domain.Bounds
import org.openmole.core.workflow.sampling.Factor
import org.openmole.core.workflow.tools.FromContext
import util.Try

import scala.annotation.tailrec
import scalaz._
import Scalaz._

object InputConverter {

  @tailrec def scaled(scales: List[Input[_]], genome: List[Double], acc: List[Variable[_]] = Nil)(context: ⇒ Context, rng: RandomProvider): List[Variable[_]] =
    if (scales.isEmpty || genome.isEmpty) acc.reverse
    else {
      val input = scales.head
      val (variable, tail) =
        input.scaled(genome).map {
          case Scalable.ScaledScalar(p, v)   ⇒ Variable(p, v) → genome.tail
          case Scalable.ScaledSequence(p, v) ⇒ Variable(p, v) → genome.drop(input.size)
        }.from(context)(rng)

      scaled(scales.tail, tail.toList, variable :: acc)({ context + variable }, rng)
    }

}

object Scalable {

  sealed trait Scaled
  case class ScaledSequence(prototype: Prototype[Array[Double]], s: Array[Double]) extends Scaled
  case class ScaledScalar(prototype: Prototype[Double], v: Double) extends Scaled

  implicit def scalarIsScalable = new Scalable[Scalar] {
    override def prototype(t: Scalar): Prototype[_] = t.prototype
    override def size(t: Scalar): Int = 1
    override def scaled(s: Scalar)(genomePart: Seq[Double]): FromContext[Scaled] = {
      val g = genomePart.head
      assert(!g.isNaN)

      for {
        min ← s.min
        max ← s.max
      } yield {
        val sc = g.scale(min, max)
        ScaledScalar(s.prototype, sc)
      }
    }
    override def toVariable(t: Scalar)(value: Seq[Any]): Variable[_] =
      Variable.unsecure(prototype(t).toArray, value.map(_.asInstanceOf[Double]).toArray[Double])
  }

  implicit def sequenceIsScalable = new Scalable[Sequence] {
    override def prototype(t: Sequence): Prototype[_] = t.prototype
    override def size(t: Sequence): Int = math.min(t.min.size, t.max.size)
    override def scaled(s: Sequence)(genomePart: Seq[Double]): FromContext[Scaled] = {
      def scaled =
        (genomePart zip (s.min zip s.max)).toVector traverseU {
          case (g, (min, max)) ⇒
            for {
              mi ← min
              ma ← max
            } yield g.scale(mi, ma)
        }

      scaled.map { sc ⇒ ScaledSequence(s.prototype, sc.toArray) }
    }

    override def toVariable(t: Sequence)(value: Seq[Any]): Variable[_] =
      Variable.unsecure(prototype(t).toArray, value.map(_.asInstanceOf[Array[Double]]).toArray[Array[Double]])
  }

  def factorToScalar[D](f: Factor[D, Double])(implicit bounded: Bounds[D, Double]) =
    Scalar(f.prototype, bounded.min(f.domain), bounded.max(f.domain))

  implicit def factorIsScalable[D](implicit bounded: Bounds[D, Double]) = new Scalable[Factor[D, Double]] {
    override def prototype(t: Factor[D, Double]): Prototype[_] = scalarIsScalable.prototype(factorToScalar(t))
    override def size(t: Factor[D, Double]): Int = scalarIsScalable.size(factorToScalar(t))
    override def scaled(t: Factor[D, Double])(genomePart: Seq[Double]): FromContext[Scaled] =
      scalarIsScalable.scaled(factorToScalar(t))(genomePart)
    override def toVariable(t: Factor[D, Double])(value: Seq[Any]): Variable[_] =
      scalarIsScalable.toVariable(factorToScalar(t))(value)
  }

}

trait Scalable[T] {
  def prototype(t: T): Prototype[_]
  def size(t: T): Int
  def scaled(t: T)(genomePart: Seq[Double]): FromContext[Scalable.Scaled]
  def toVariable(t: T)(value: Seq[Any]): Variable[_]
}

case class Scalar(prototype: Prototype[Double], min: FromContext[Double], max: FromContext[Double])

object Sequence {
  def apply(prototype: Prototype[Array[Double]], min: FromContext[Double], max: FromContext[Double], size: Int): Sequence =
    Sequence(prototype, Seq.fill(size)(min), Seq.fill(size)(max))
}

case class Sequence(prototype: Prototype[Array[Double]], min: Seq[FromContext[Double]], max: Seq[FromContext[Double]])

object Input {
  implicit def toInput[T: Scalable](t: T): Input[T] = new Input(t, implicitly[Scalable[T]])
}

class Input[T](t: T, scalable: Scalable[T]) {
  def prototype = scalable.prototype(t)
  def size = scalable.size(t)
  def scaled(genomePart: Seq[Double]) = scalable.scaled(t)(genomePart)
  def toVariable(value: Seq[Any]): Variable[_] = scalable.toVariable(t)(value)
}

object Genome {
  def apply(inputs: Input[_]*) = UniqueGenome(inputs)
}

object UniqueGenome {
  def size(g: UniqueGenome) = g.inputs.map(_.size).sum

  def apply(inputs: Genome): UniqueGenome = {
    val prototypes = inputs.map(_.prototype).distinct
    new UniqueGenome(prototypes.map(p ⇒ inputs.reverse.find(_.prototype == p).get))
  }

  implicit def genomeToSeqOfInput(g: UniqueGenome): Seq[Input[_]] = g.inputs
}

class UniqueGenome(val inputs: Seq[Input[_]]) extends AnyVal