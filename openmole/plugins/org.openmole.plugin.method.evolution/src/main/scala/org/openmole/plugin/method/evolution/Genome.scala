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

  @tailrec def scaled(scales: List[Input], genome: List[Double], acc: List[Variable[_]] = Nil)(context: ⇒ Context, rng: RandomProvider): List[Variable[_]] =
    if (scales.isEmpty || genome.isEmpty) acc.reverse
    else {
      val input = scales.head
      val (variable, tail) =
        scaled(input, genome).map {
          case ScaledScalar(p, v) ⇒
            assert(!v.isNaN); Variable(p, v) -> genome.tail
          case ScaledSequence(p, v) ⇒ Variable(p, v) -> genome.drop(input.size)
        }.from(context)(rng)

      scaled(scales.tail, tail.toList, variable :: acc)({ context + variable }, rng)
    }

  def scaled(input: Input, genomePart: Seq[Double]): FromContext[Scaled] =
    input match {
      case s @ Scalar(p, _, _) ⇒
        val g = genomePart.head
        assert(!g.isNaN)

        for {
          min ← s.min
          max ← s.max
        } yield {
          val sc = g.scale(min, max)
          ScaledScalar(s.prototype, sc)
        }
      case s @ Sequence(p, _, _) ⇒
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

  sealed trait Scaled
  case class ScaledSequence(prototype: Prototype[Array[Double]], s: Array[Double]) extends Scaled
  case class ScaledScalar(prototype: Prototype[Double], v: Double) extends Scaled

}

object Input {

  implicit def doubleBoundsToInput[D](f: Factor[Double, D])(implicit bounded: Bounds[Double, D]) =
    Scalar(f.prototype, bounded.min(f.domain), bounded.max(f.domain))

}

sealed trait Input {
  def prototype: Prototype[_]
  def size: Int
}

case class Scalar(prototype: Prototype[Double], min: FromContext[Double], max: FromContext[Double]) extends Input {
  def size = 1
}

object Sequence {
  def apply(prototype: Prototype[Array[Double]], min: FromContext[Double], max: FromContext[Double], size: Int): Sequence =
    Sequence(prototype, Seq.fill(size)(min), Seq.fill(size)(max))
}

case class Sequence(prototype: Prototype[Array[Double]], min: Seq[FromContext[Double]], max: Seq[FromContext[Double]]) extends Input {
  def size = math.min(min.size, max.size)
}

case class Genome(inputs: Input*) {
  def size: Int = Try(inputs.map(_.size).sum).getOrElse(0)
}
