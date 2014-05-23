/*
 * Copyright (C) 11/06/13 Romain Reuillon
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

import org.openmole.core.model.data._
import org.openmole.misc.tools.script._
import org.openmole.core.implementation.tools._
import fr.iscpif.mgo._
import org.openmole.plugin.method.evolution.{ Sequence, Scalar, Input, Inputs }

object GenomeScaling {

  def scaled(scales: List[(Input, (GroovyFunction, GroovyFunction))], genome: List[Double], context: Context): List[Variable[_]] =
    if (scales.isEmpty || genome.isEmpty) List.empty
    else {
      val sc = scaled(scales.head, context, genome)
      val (variable, tail) =
        sc match {
          case ScaledScalar(s, v)   ⇒ Variable(s.prototype, v) -> genome.tail
          case ScaledSequence(s, v) ⇒ Variable(s.prototype, v) -> genome.drop(s.size)
        }

      variable :: scaled(scales.tail, tail.toList, context + variable)
    }

  def scaled(input: (Input, (GroovyFunction, GroovyFunction)), context: Context, genomePart: Seq[Double]) = {
    val (i, (vMin, vMax)) = input
    val dMin = vMin(context).toString.toDouble
    val dMax = vMax(context).toString.toDouble

    i match {
      case s @ Scalar(p, _, _)         ⇒ ScaledScalar(s, genomePart.head.scale(dMin, dMax))
      case s @ Sequence(p, _, _, size) ⇒ ScaledSequence(s, genomePart.take(size).toArray.map(_.scale(dMin, dMax)))
    }
  }

  sealed trait Scaled
  case class ScaledSequence(input: Sequence, s: Array[Double]) extends Scaled
  case class ScaledScalar(input: Scalar, v: Double) extends Scaled

  def groovyProxies(inputs: Inputs) =
    inputs.inputs.map {
      case s @ Scalar(_, min, max)      ⇒ s -> (GroovyProxyPool(min), GroovyProxyPool(max))
      case s @ Sequence(_, min, max, _) ⇒ s -> (GroovyProxyPool(min), GroovyProxyPool(max))
    }

}

trait GenomeScaling {

  def scales: Inputs

  def scaled(genome: Seq[Double], context: Context) = GenomeScaling.scaled(groovyScales.toList, genome.toList, context)

  @transient lazy val groovyScales = GenomeScaling.groovyProxies(scales)

}
