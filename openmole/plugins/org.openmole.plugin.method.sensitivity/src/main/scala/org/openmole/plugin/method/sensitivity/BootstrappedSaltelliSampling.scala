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
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service.Random._

import SaltelliSampling._

class BootstrappedSaltelliSampling(
  val matrixName: IPrototype[String],
  val variables: Seq[IPrototype[Double]],
  val seed: IPrototype[Long]) extends SaltelliSampling {
  
  def this(matrix: IPrototype[String], factors: Array[IPrototype[Double]], seed: IPrototype[Long]) = 
    this(matrix, factors.toSeq, seed)
  
  override def inputs = new DataSet(seed, matrixName :: variables.map(p => toArray(p)).toList)
  
  def factorsPrototypes = variables
  
  override def a(context: IContext) = shuffleMatrix(context, aMatrixName)
  override def b(context: IContext) = shuffleMatrix(context, bMatrixName) 
  
  def shuffleMatrix(context: IContext, name: String) = {
    val rng = Workspace.newRNG(context.valueOrException(seed))
     val allNames = context.valueOrException(toArray(matrixName))
    variables.map {
      v =>
        val allValues = context.valueOrException(toArray(v))
        val a = extractValues(allValues, allNames, name)
        shuffled(allValues)(rng)
      }
  }
}
