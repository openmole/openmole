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

package org.openmole.plugin.sensitivity

import java.util.Random
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.domain.IBounded
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.sampling.IFactor
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service.Scaling._


class RandomSaltelliSampling(
  val samples: Int,
  val matrixName: IPrototype[String],
  val factors: Seq[IFactor[Double, IDomain[Double] with IBounded[Double]]],
  rng: Random) extends SaltelliSampling {

  def this(samples: Int, matrix: IPrototype[String], factors: Array[IFactor[Double, IDomain[Double] with IBounded[Double]]], seed: Long) =
    this(samples, matrix, factors, Workspace.newRNG(seed))
  
  def this(samples: Int, matrix: IPrototype[String], factors: Array[IFactor[Double, IDomain[Double] with IBounded[Double]]]) =
    this(samples, matrix, factors, Workspace.newRNG)
  
  
  def generateMatrix(context: IContext) = 
    for(i <- 0 until samples) yield factors.map { f => rng.nextDouble.scale(f.domain.min(context), f.domain.max(context)) }.toSeq
  
}
