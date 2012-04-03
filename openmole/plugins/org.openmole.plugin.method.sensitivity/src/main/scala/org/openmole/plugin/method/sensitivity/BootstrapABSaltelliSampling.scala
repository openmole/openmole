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

package org.openmole.plugin.method.sensitivity

import java.util.Random
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.misc.workspace.Workspace
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.domain.IBounded
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.sampling.IFactor
import org.openmole.misc.tools.service.Random._

import SaltelliSampling._

object BootstrapABSaltelliSampling {
  def aPrototype(p: IPrototype[Double]) = new Prototype(p.name + "A", classOf[Array[Double]])
  def bPrototype(p: IPrototype[Double]) = new Prototype(p.name + "B", classOf[Array[Double]])
}

import BootstrapABSaltelliSampling._

class BootstrapABSaltelliSampling(
  samples: Int,
  nbBootStrap: Int,
  factors: Array[IFactor[Double, IDomain[Double] with IBounded[Double]]],
  rng: Random
) extends Sampling {
  
  override def prototypes = factors.map{f => aPrototype(f.prototype)} ++ factors.map{f => bPrototype(f.prototype)}
    
  override def build(context: IContext): Iterator[Iterable[IVariable[_]]] = {
    val a = generateMatrix(context, samples, factors, rng)
    val b = generateMatrix(context, samples, factors, rng)

    val ab = generateVariables(a, b) 
    val abBootstraped = 
      (0 until nbBootStrap).map{ bootstrap => generateVariables(shuffleMatrix(a), shuffleMatrix(b))}.toList
    (ab :: abBootstraped).iterator
  }
  
  
  def generateVariables(a: Array[Array[Double]], b: Array[Array[Double]]) = (aToVariables(a) ++ bToVariables(b)).toIterable
  
  
  def aToVariables(a: Array[Array[Double]]) = 
    factors zip a map { case(f, l) => new Variable(aPrototype(f.prototype), l)}
    
  def bToVariables(b: Array[Array[Double]]) = 
    factors zip b map { case(f, l) => new Variable(bPrototype(f.prototype), l)}
  
  
  def shuffleMatrix(matrix: Array[Array[Double]]): Array[Array[Double]] = 
    matrix.map { shuffled(_)(rng).toArray }
}
