/*
 *
 *  Copyright (c) 2007, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.plugin.plan.lhs;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import org.openmole.core.workflow.implementation.plan.ExploredPlan;


import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.service.RNG;
import org.openmole.core.workflow.implementation.plan.FactorsValues;
import org.openmole.core.workflow.implementation.plan.Plan;
import org.openmole.core.workflow.model.plan.IFactor;
import org.openmole.core.workflow.model.plan.IFactorValues;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.domain.IDiscretizedIntervalDomain;
import org.openmole.core.workflow.model.plan.IExploredPlan;


public class LHSPlan extends Plan<IFactor<? super Double, IDiscretizedIntervalDomain<? extends Double>>> {

    private int nbOfExperiments;

    public LHSPlan() {
        this(64);
    }

    public LHSPlan(int nbOfExperiments) {
        super();
        this.nbOfExperiments = nbOfExperiments;
    }

    public int getNbOfExperiments() {
        return nbOfExperiments;
    }

    public void setNbOfExperiments(int nbOfExperiments) {
        this.nbOfExperiments = nbOfExperiments;
    }
    // n number of experiments
    // k number of factors
    // dist distribution : in the factor domain 

    @Override
    public IExploredPlan build(IContext context) throws InternalProcessingError, UserBadDataError {
     
        //System.out.println("LHSPlan::computeValues");
        //Inititalize a temp structure
        List<List<Double>> TempFactors = new ArrayList<List<Double>>(getFactors().size());//(ArrayList<Double>[])new ArrayList[nbOfExperiments]; //new  List<Double>[nbOfExperiments] ;  //ArrayList<Double>(nbOfExperiments)[nbOfExperiments]; //= new double[getExperimentalDesign().getFactors().size()][nbOfExperiments] ;
        for(int i = 0 ; i < getFactors().size() ; i++)
        {
            TempFactors.add(new ArrayList<Double>(nbOfExperiments));
        }




        //System.out.println("LHSPlan::computeValues  Sampling step.");
        for (int j = 0; j < nbOfExperiments; j++) {
            int i = 0;
            for (IFactor<? super Double, IDiscretizedIntervalDomain<? extends Double>> f : getFactors()) {
                Double tempMin = f.getDomain().getInterval().getMin(context);
                Double tempMax = f.getDomain().getInterval().getMax(context);
                TempFactors.get(i).add( ((j + RNG.getRng().nextDouble()) / nbOfExperiments) * (tempMax - tempMin) + tempMin  );
                i++;
            }
        }
        //System.out.println("LHSPlan::computeValues  Shuffling step.");
        for (int i = 0; i < getFactors().size(); i++) {
           Collections.shuffle(TempFactors.get(i));
        }
        // TODO : TempFactors is now centered and reduced. It must be corrected according to factor's parameters.
    
       
        //System.out.println("LHSPlan::computeValues return computed values.");
        // affect computed values and names to the plan
        List<IFactorValues> listOfListOfValues = new ArrayList<IFactorValues>();

        for (int j = 0; j < nbOfExperiments; j++) {
            FactorsValues factorValues = new FactorsValues();
            int i = 0;
            for (IFactor<? super Double, IDiscretizedIntervalDomain<? extends Double>> f : getFactors()) {

                factorValues.setValue(f.getPrototype(), TempFactors.get(i).get(j));
                //System.out.println("LHSPlan::computeValues " + TempFactors.get(i).get(j));
                i++;
            }
            listOfListOfValues.add(factorValues);
        }
             //System.out.println("LHSPlan::computeValues  end.");
        return new ExploredPlan(listOfListOfValues);
    }
        
}