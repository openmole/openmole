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

import fr.iscpif.mgo.ga.algorithm.GAGenomeWithSigma
import fr.iscpif.mgo.ga.algorithm.GAGenomeWithSigmaFactory
import java.util.Random
import org.openmole.core.implementation.data.Variable
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.sampling.ISampling
import org.openmole.misc.workspace.Workspace

class SigmaGenomeSampling(genome: IPrototype[GAGenomeWithSigma], genomeSize: Int, nbGenome: Int, generator: Random) extends ISampling {
  
  def this(genome: IPrototype[GAGenomeWithSigma], genomeSize: Int, nbGenome: Int) = this(genome, genomeSize, nbGenome, Workspace.newRNG)
  
  @transient lazy val factory = new GAGenomeWithSigmaFactory(genomeSize)
  
  def prototypes = List(genome)

  def build(context: IContext) = 
    (0 until nbGenome).map{ i => List(new Variable(genome, factory.random(generator))) }.iterator
}
