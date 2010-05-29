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

package org.openmole.plugin.plan.centralcompositeplan

import org.openmole.core.workflow.implementation.plan.Plan
import org.openmole.core.workflow.model.domain.IDiscretizedIntervalDomain
import org.openmole.core.workflow.model.domain.IDomainWithCenter
import org.openmole.core.workflow.model.domain.IDomainWithRange
import org.openmole.core.workflow.model.plan.IFactor
import org.openmole.core.workflow.model.plan.IFactorValues
import org.openmole.core.workflow.model.job.IContext
import org.openmole.core.workflow.model.plan.IExploredPlan
import org.openmole.core.workflow.implementation.plan.FactorsValues
import org.openmole.core.workflow.implementation.plan.Plan
import org.openmole.core.workflow.implementation.plan.ExploredPlan

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._

/**
 *
 * @author thierry
 * Montgomery propose alpha=(nF)^0.25 where nF is the number of pints used in the
 * factorial portion of the design
 * If alpha=1 we have a face-centered composite design (each aditional point is
 * centered in the middle of the face (It requires only 3 levels for each factor)
 */
class CentralCompositePlan(nbCenterPoint: Int, nbReplicatesForFactorialPoint: Int, nbReplicatesForAxialPoint: Int, alpha: Int) extends Plan[IFactor[Double,IDiscretizedIntervalDomain[Double] with IDomainWithRange[Double] with IDomainWithCenter[Double]]] {

    val getNbCenterPoint = nbCenterPoint
    val getNbReplicatesForFactorialPoint = nbReplicatesForFactorialPoint
    val getNbReplicatesForAxialPoint = nbReplicatesForAxialPoint
    val getAlpha = alpha

    override def build(context: IContext) : IExploredPlan = {

        val alpha = Math.pow(Math.pow(2, getFactors().size()), this.alpha)

        val factorNames = new ArrayBuffer[String](getFactors() size)
        val listOfListOfValues = new ArrayBuffer[IFactorValues]()

        //  set factor names
        getFactors().foreach ( f => {
            factorNames.add(f.getPrototype.getName)
        })

        // add 2*factors.size() in the axis to compute curvature
        getFactors.foreach (f => {
            val range = f.getDomain().getRange(context)
            // add min value
            val t0Min = f.getDomain().getInterval().getMin(context) - range * alpha

            val factorValuesMin =  new FactorsValues()
            getFactors().foreach( f1 =>  {


                if (f.equals(f1)) {
                    factorValuesMin.setValue(f.getPrototype(), t0Min)

                } else {
                    factorValuesMin.setValue(f.getPrototype(), 0.0)
                }
            })

            listOfListOfValues.add(factorValuesMin);

// add max values
            val t0Max = f.getDomain().getInterval().getMax(context) + range * alpha
            val factorValuesMax =  new FactorsValues()
            getFactors().foreach( f1 => {
                if (f.equals(f1)) {
                    factorValuesMax.setValue(f.getPrototype(), t0Max)
                } else {
                    factorValuesMax.setValue(f.getPrototype(), 0.0)
                }
            })
            listOfListOfValues.add(factorValuesMax);
        })

        // add nbCenterPoints to compute error
        for (i <- 0 to nbCenterPoint) {
            val factorValues =  new FactorsValues()
            getFactors().foreach( f1 => {
                factorValues.setValue(f1.getPrototype(), f1.getDomain().getCenter(context))
            })
            listOfListOfValues.add(factorValues)
        }

        new ExploredPlan(listOfListOfValues)

    }

}
