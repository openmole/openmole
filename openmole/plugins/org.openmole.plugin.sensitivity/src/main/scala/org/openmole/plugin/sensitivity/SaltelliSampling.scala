/*
 * Copyright (C) 2010 reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.sensitivity

import org.openmole.core.model.sampling.ISampling
import org.openmole.misc.tools.service.Scaling._
import org.openmole.misc.tools.service.Random._
import java.util.Random
import org.openmole.core.implementation.data.Variable
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IBounded
import org.openmole.core.model.sampling.IFactor

object SaltelliSampling {
  val aMatrixName = "a"
  val bMatrixName = "b"
  def cMatrixName(p: String) = "c" + p
}

import SaltelliSampling._

abstract class SaltelliSampling extends ISampling {

  def samples: Int
  def factors: Seq[IFactor[Double, IDomain[Double] with IBounded[Double]]]
  def matrixName: IPrototype[String]
  
  override def prototypes = factors.map{_.prototype}
  
  def generateMatrix(context: IContext): Iterable[Iterable[Double]]
  
  override def build(context: IContext): Iterator[Iterable[IVariable[_]]] = {

    val a = generateMatrix(context)
    val b = generateMatrix(context)


    val cMatrix = 
      prototypes.zipWithIndex.flatMap {
        case(p, i) => toVariables(buildC(i, a, b), cMatrixName(p.name)) 
      }
    
    (toVariables(a, aMatrixName) ++ toVariables(b, bMatrixName) ++ cMatrix).iterator
  }
  
  def toVariables(matrix: Iterable[Iterable[Double]], m: String): List[Iterable[IVariable[_]]] = 
      matrix.map {
        l => new Variable(matrixName, m) :: (l zip factors map {  case(v, f) => new Variable(f.prototype, v) }).toList
      }.toList
  
  def buildC(
    i: Int,
    a: Iterable[Iterable[Double]],
    b: Iterable[Iterable[Double]]) =
    a zip b map { 
      case(lineOfA, lineOfB) => buildLineOfC(i, lineOfA, lineOfB)
    }
  
  
  def buildLineOfC(i: Int, lineOfA: Iterable[Double], lineOfB: Iterable[Double]) = 
    (lineOfA zip lineOfB zipWithIndex) map {
      case ((a, b), index) => if(index == i) a else b
    }
  
}
