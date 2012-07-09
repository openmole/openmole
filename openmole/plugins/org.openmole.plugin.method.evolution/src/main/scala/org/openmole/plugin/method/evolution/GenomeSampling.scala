/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo.ga._
import fr.iscpif.mgo._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.core.model.data.DataModeMask
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.service.Random._
import org.openmole.misc.workspace.Workspace
import org.openmole.core.implementation.task.Task._

class GenomeSampling[G <: Genome](
    genome: IPrototype[G],
    size: Int)(implicit factory: Factory[G]) extends Sampling {

  def prototypes = List(genome)

  def build(context: IContext) = {
    def toSamplingLine(g: G) = List(new Variable(genome, g))

    val rng = newRNG(context.valueOrException(openMOLESeed))

    ((0 until size).map(i ⇒ toSamplingLine(factory.random(rng)))).iterator
  }
}
