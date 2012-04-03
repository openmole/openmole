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


import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable

import BootstrapABSaltelliSampling._
import SaltelliSampling._


class CFromBootstrapSaltelliSampling(
  matrixName: IPrototype[String],
  factors: Array[IPrototype[Double]]
) extends Sampling {
  
  override def inputs = DataSet(factors.map{f => aPrototype(f)} ++ factors.map{f => bPrototype(f)})
  override def prototypes = matrixName :: factors.toList 
    
  override def build(context: IContext): Iterator[Iterable[IVariable[_]]] = {
    val a = reconstructA(context)
    val b = reconstructB(context)
        
    val cMatrix = 
      factors.zipWithIndex.flatMap {
        case(f, i) => toVariables(buildC(i, a, b), cMatrixName(f.name), factors, matrixName) 
      }
    
    (toVariables(a, aMatrixName, factors, matrixName) ++ toVariables(b, bMatrixName, factors, matrixName) ++ cMatrix).iterator
  }
  
  
  def reconstructA(context: IContext): Array[Array[Double]] = factors.map{f => context.valueOrException(aPrototype(f))}
  def reconstructB(context: IContext): Array[Array[Double]] = factors.map{f => context.valueOrException(bPrototype(f))}
}
