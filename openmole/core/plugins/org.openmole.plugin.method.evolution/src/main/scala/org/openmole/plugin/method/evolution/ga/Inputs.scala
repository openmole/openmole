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

  implicit val doubleInputConverter =
    new InputConverter[Double] {
      override def converter(v: Input[Double]): ToDouble =
        new ToDouble {
          override def toDouble(context: Context) = (v.min, v.max)
        }
    }

  implicit val stringInputConverter =
    new InputConverter[String] {
      override def converter(v: Input[String]): ToDouble =
        new ToDouble {
          @transient lazy val proxy = groovyProxy(v)
          override def toDouble(context: Context) = {
            val (p1, p2) = proxy
            (p1(context).toString.toDouble, p2(context).toString.toDouble)
          }
        }
    }

  def scaled(scales: List[(Input[_], ToDouble)], genome: List[Double], context: Context): List[Variable[_]] =
    if (scales.isEmpty || genome.isEmpty) List.empty
    else {
      val (input, toDouble) = scales.head
      val (variable, tail) =
        scaled(input, toDouble, context, genome) match {
          case ScaledScalar(p, v)   ⇒ Variable(p, v) -> genome.tail
          case ScaledSequence(p, v) ⇒ Variable(p, v) -> genome.drop(input.size)
        }

      variable :: scaled(scales.tail, tail.toList, context + variable)
    }

  def scaled(input: Input[_], toDouble: ToDouble, context: Context, genomePart: Seq[Double]) = {
    val (min, max) = toDouble.toDouble(context)

    input match {
      case s @ Scalar(p, _, _)         ⇒ ScaledScalar(s.prototype, genomePart.head.scale(min, max))
      case s @ Sequence(p, _, _, size) ⇒ ScaledSequence(s.prototype, genomePart.take(size).toArray.map(_.scale(min, max)))
    }
  }

  sealed trait Scaled
  case class ScaledSequence(prototype: Prototype[Array[Double]], s: Array[Double]) extends Scaled
  case class ScaledScalar(prototype: Prototype[Double], v: Double) extends Scaled

  def groovyProxy(input: Input[String]) =
    input match {
      case s @ Scalar(_, min, max)      ⇒ (GroovyProxyPool(min), GroovyProxyPool(max))
      case s @ Sequence(_, min, max, _) ⇒ (GroovyProxyPool(min), GroovyProxyPool(max))
    }

}

trait InputsConverter {
  type INPUT

  def inputs: Inputs[INPUT]
  def inputConverter: InputConverter[INPUT]

  @transient lazy val toDoubles: Seq[(Input[INPUT], ToDouble)] = inputs.inputs.map(i ⇒ i -> inputConverter.converter(i))

  def scaled(genome: Seq[Double], context: Context): List[Variable[_]] = {
    InputConverter.scaled(toDoubles.toList, genome.toList, context)
  }

}

trait ToDouble {
  def toDouble(context: Context): (Double, Double)
}

trait InputConverter[T] {
  def converter(v: Input[T]): ToDouble
}

sealed trait Input[T] {
  def min: T
  def max: T
  def prototype: Prototype[_]
  def size: Int
}

case class Scalar[T](prototype: Prototype[Double], min: T, max: T) extends Input[T] {
  def size = 1
}

case class Sequence[T](prototype: Prototype[Array[Double]], min: T, max: T, size: Int) extends Input[T]

case class Inputs[T](inputs: Seq[Input[T]]) {
  def size: Int = Try(inputs.map(_.size).sum).getOrElse(0)
}
