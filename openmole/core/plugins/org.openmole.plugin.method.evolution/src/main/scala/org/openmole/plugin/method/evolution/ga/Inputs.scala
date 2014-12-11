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

package org.openmole.plugin.method.evolution.ga

import org.openmole.core.model.data.{ Variable, Context, Prototype }
import org.openmole.misc.tools.script.{ GroovyProxyPool, GroovyFunction }
import fr.iscpif.mgo.double2Scalable
import org.openmole.core.implementation.tools._
import util.Try

object InputConverter {

  def scaled(scales: List[Input], genome: List[Double], context: ⇒ Context): List[Variable[_]] =
    if (scales.isEmpty || genome.isEmpty) List.empty
    else {
      val input = scales.head
      val (variable, tail) =
        scaled(input, context, genome) match {
          case ScaledScalar(p, v) ⇒
            assert(!v.isNaN); Variable(p, v) -> genome.tail
          case ScaledSequence(p, v) ⇒ Variable(p, v) -> genome.drop(input.size)
        }

      variable :: scaled(scales.tail, tail.toList, context + variable)
    }

  def scaled(input: Input, context: ⇒ Context, genomePart: Seq[Double]) = {
    val min = input.min.from(context)
    val max = input.max.from(context)

    input match {
      case s @ Scalar(p, _, _) ⇒
        val g = genomePart.head
        assert(!g.isNaN)
        val sc = g.scale(min, max)
        ScaledScalar(s.prototype, sc)
      case s @ Sequence(p, _, _, size) ⇒ ScaledSequence(s.prototype, genomePart.take(size).toArray.map(_.scale(min, max)))
    }
  }

  sealed trait Scaled
  case class ScaledSequence(prototype: Prototype[Array[Double]], s: Array[Double]) extends Scaled
  case class ScaledScalar(prototype: Prototype[Double], v: Double) extends Scaled

}

trait InputsConverter {

  def inputs: Inputs

  def scaled(genome: Seq[Double], context: Context): List[Variable[_]] = {
    InputConverter.scaled(inputs.inputs.toList, genome.toList, context)
  }

}

sealed trait Input {
  def min: FromContext[Double]
  def max: FromContext[Double]
  def prototype: Prototype[_]
  def size: Int
}

case class Scalar(prototype: Prototype[Double], min: FromContext[Double], max: FromContext[Double]) extends Input {
  def size = 1
}

case class Sequence(prototype: Prototype[Array[Double]], min: FromContext[Double], max: FromContext[Double], size: Int) extends Input

case class Inputs(inputs: Seq[Input]) {
  def size: Int = Try(inputs.map(_.size).sum).getOrElse(0)
}
