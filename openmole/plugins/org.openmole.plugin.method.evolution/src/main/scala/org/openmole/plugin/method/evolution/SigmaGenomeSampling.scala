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
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.workspace.Workspace


class SigmaGenomeSampling(
  genome: IPrototype[GAGenomeWithSigma],
  genomeSize: Int,
  nbGenome: Int,
  generator: Random,
  initialGenomes: Array[Array[Double]] = Array.empty
) extends Sampling {
  
  def this(genome: IPrototype[GAGenomeWithSigma], genomeSize: Int, nbGenome: Int) = this(genome, genomeSize, nbGenome, Workspace.newRNG)
  
  def this(genome: IPrototype[GAGenomeWithSigma], genomeSize: Int, nbGenome: Int, initialPopulation: Array[Array[Double]]) = 
    this(genome, genomeSize, nbGenome, Workspace.newRNG, initialPopulation)

  @transient lazy val factory = new GAGenomeWithSigmaFactory(genomeSize)
  
  {
    val wrongSize = 
      initialGenomes.filter(_.size != genomeSize).map {
        i => "(" + i.mkString(",") + ")"
      }
    if(wrongSize.size != 0) throw new UserBadDataError("Genomes doesn't have the correct size " + genomeSize + ": " + wrongSize.mkString(";"))
  }
    
    def prototypes = List(genome)

  def build(context: IContext) = {
    def toSamplingLine(g: GAGenomeWithSigma) =  List(new Variable(genome, g))
    
    val genomes = 
      initialGenomes.map {
        g => 
          val rg = factory.random(generator)
          toSamplingLine(factory.updatedValues(rg, g))
      }.take(nbGenome)
      
    (genomes ++ 
     (0 until genomes.size - nbGenome).map{ i => toSamplingLine(factory.random(generator))}
    ).iterator
  }
}
