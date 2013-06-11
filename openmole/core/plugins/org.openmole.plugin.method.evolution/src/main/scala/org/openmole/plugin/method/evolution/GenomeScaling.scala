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

package org.openmole.plugin.method.evolution

import org.openmole.core.model.data._
import org.openmole.misc.tools.script._
import org.openmole.core.implementation.tools._
import fr.iscpif.mgo._

object GenomeScaling {

  def scaled(scales: List[(Prototype[Double], (GroovyFunction, GroovyFunction))], genome: List[Double], context: Context): List[Variable[Double]] =
    if (scales.isEmpty || genome.isEmpty) List.empty
    else {
      val (p, (vMin, vMax)) = scales.head
      val dMin = vMin(context).toString.toDouble
      val dMax = vMax(context).toString.toDouble
      val scaledV = Variable(p, genome.head.scale(dMin, dMax))
      scaledV :: scaled(scales.tail, genome.tail, context + scaledV)
    }

  def groovyProxies(scales: Traversable[(Prototype[Double], (String, String))]) =
    scales.map { case (p, (min, max)) ⇒ p -> (GroovyProxyPool(min), GroovyProxyPool(max)) }

  type Scale = (Prototype[Double], (String, String))

}

trait GenomeScaling {

  def scales: Seq[GenomeScaling.Scale]
  def scaled(genome: Seq[Double], context: Context) = GenomeScaling.scaled(groovyScales.toList, genome.toList, context)

  @transient lazy val groovyScales = GenomeScaling.groovyProxies(scales)

}
