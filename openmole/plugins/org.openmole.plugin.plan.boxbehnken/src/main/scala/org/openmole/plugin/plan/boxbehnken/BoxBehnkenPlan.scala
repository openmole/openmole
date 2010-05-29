/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.plan.boxbehnken

import org.openmole.core.implementation.plan.Plan
import org.openmole.core.model.domain.IDiscretizedIntervalDomain
import org.openmole.core.model.domain.IDomainWithCenter
import org.openmole.core.model.domain.IDomainWithRange
import org.openmole.core.model.plan.IFactor
import org.openmole.core.model.plan.IFactorValues
import org.openmole.core.model.job.IContext
import org.openmole.core.model.plan.IExploredPlan
import org.openmole.core.implementation.plan.FactorsValues
import org.openmole.core.implementation.plan.Plan
import org.openmole.core.implementation.plan.ExploredPlan

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._
/**
 * Box and Behnken (1960) : Some new three level designs for the study of quantitative
 * variables, Technometrics, Vol. 2, PP 455-476.
 * Three level design for fitting response surfaces :
 * These designs are formed by combining 2^k factorials with incomplete block designs.
 * The resulting designs are usually very efficient in terms of the number of required
 * runs, and they are either rotatable ore nearly rotatable.
 * nbCenterPoint (nb points to compute error in the design) set to 4 by default  (Montgomery advise 3 to 5)
 * @author thierry
 */
class BoxBehnkenPlan(nbCenterPoint: Int = 3) extends Plan[IFactor[Double,IDiscretizedIntervalDomain[Double] with IDomainWithCenter[Double]]] {


    override def build(context: IContext): IExploredPlan = {

        val listOfListOfValues = ArrayBuffer[IFactorValues]()

        val tabMin = Array[Double](getFactors().size())
        val tabMax = Array[Double](getFactors().size())

        var i = 0

        getFactors().foreach( f => {
            tabMin(i) = f.getDomain().getInterval().getMin(context)
            tabMax(i) = f.getDomain().getInterval().getMax(context)
            i += 1
        })

        // add nbCenterPoints to compute error
        for (i <- 0 to nbCenterPoint) {

            val factorValues = new FactorsValues();

            getFactors().foreach( f1 => {
                val v = f1.getDomain().getCenter(context)
                factorValues.setValue(f1.getPrototype(), v)
            })
            listOfListOfValues += factorValues
        }

        val factorArray = new Array[IFactor[Double,IDiscretizedIntervalDomain[Double] with IDomainWithCenter[Double]]](getFactors().size())

        i = 0;
        getFactors().foreach( factorArray(i) = _ )

        // add other part of the design
        for (i <- 0 to getFactors().size() - 1) {
            for (j <- i to getFactors().size()) {
                for (k <- 0 to 4) {

                    val factorValues = new FactorsValues();

                    getFactors().foreach( f => {factorValues.setValue(f.getPrototype(), f.getDomain().getCenter(context))})

                    k match {
                        case 0 =>
                            factorValues.setValue(factorArray(i).getPrototype(), tabMax(i))
                            factorValues.setValue(factorArray(j).getPrototype(), tabMax(j))
                            //newValues.set(i, tabMax[i]);
                            //newValues.set(j, tabMax[j]);
                        case 1 =>
                            factorValues.setValue(factorArray(i).getPrototype(), tabMax(i))
                            factorValues.setValue(factorArray(j).getPrototype(), tabMin(j))
                            //newValues.set(i, tabMax[i]);
                            //newValues.set(j, tabMin[j]);
                        case 2 =>
                            factorValues.setValue(factorArray(i).getPrototype(), tabMin(i))
                            factorValues.setValue(factorArray(j).getPrototype(), tabMax(j))
                            //newValues.set(i, tabMin[i]);
                            //newValues.set(j, tabMax[j]);
                        case 3 =>
                            factorValues.setValue(factorArray(i).getPrototype(), tabMin(i))
                            factorValues.setValue(factorArray(j).getPrototype(), tabMin(j))
                            //newValues.set(i, tabMin[i]);
                            //newValues.set(j, tabMin[j]);
                      //  default:
                    }
                    listOfListOfValues.add(factorValues);
                }

            }
        }
        new ExploredPlan(listOfListOfValues)
    }


}
