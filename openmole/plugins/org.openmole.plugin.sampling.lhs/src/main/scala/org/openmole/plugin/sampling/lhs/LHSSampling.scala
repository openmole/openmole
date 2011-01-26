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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.sampling.lhs

import java.util.Arrays
import java.util.Collections
import org.openmole.commons.tools.service.RNG
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IWithRange
import org.openmole.core.model.sampling.IFactor
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions
import scala.collection.mutable.ListBuffer

class LHSSampling(samples: Int, factors: Array[IFactor[Double, IDomain[Double] with IWithRange[Double]]]) extends Sampling[IFactor[Double, IDomain[Double] with IWithRange[Double]]](factors) {

  override def build(context: IContext): Iterable[Iterable[IVariable[Double]]] = {
     
    //System.out.println("LHSPlan::computeValues");
    //Inititalize a temp structure
    val TempFactors = new Array[ArrayBuffer[Double]](factors.size)//(ArrayList<Double>[])new ArrayList[nbOfExperiments]; //new  List<Double>[nbOfExperiments] ;  //ArrayList<Double>(nbOfExperiments)[nbOfExperiments]; //= new double[getExperimentalDesign().getFactors().size()][nbOfExperiments] ;
    for(i <- 0 until factors.size) {
      TempFactors(i) = new ArrayBuffer[Double](samples)
    }

    //System.out.println("LHSPlan::computeValues  Sampling step.");
    for (j <- 0 until samples) {
      var i = 0
      for (f <- factors) {
        val tempMin = f.domain.min(context)
        val tempMax = f.domain.max(context)
        TempFactors(i) += ( ((j + RNG.nextDouble) / samples) * (tempMax - tempMin) + tempMin)
        i += 1
      }
    }

    for (i <- 0 until factors.size) RNG.shuffle(TempFactors(i))
    
    // TODO : TempFactors is now centered and reduced. It must be corrected according to factor's parameters.
    // affect computed values and names to the plan
    val listOfListOfValues = new Array[Iterable[IVariable[Double]]](samples)

    for (j <- 0 until  samples) {
      val factorValues = new ListBuffer[IVariable[Double]]
      var i = 0
            
      for (f <- factors) {
        factorValues += new Variable(f.prototype, TempFactors(i)(j))
        i += 1
      }
      listOfListOfValues(j) = factorValues
    }
    //System.out.println("LHSPlan::computeValues  end.");
    listOfListOfValues
  }
}
