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
package org.openmole.core.workflow.methods.r;

import org.openmole.core.workflow.implementation.plan.FactorsValues;
import org.openmole.core.workflow.model.plan.IFactor;
import org.openmole.core.workflow.model.plan.IFactorValues;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.task.annotations.Input;

import java.util.ArrayList;
import java.util.List;
import org.openmole.core.workflow.implementation.plan.ExploredPlan;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RserveException;
import org.openmole.core.workflow.implementation.plan.Plan;
import org.openmole.core.workflow.implementation.data.Data;
import org.openmole.core.workflow.model.plan.IExploredPlan;
import org.openmole.core.workflow.model.domain.IDiscretizedIntervalDomain;

public class RFastMethod extends Plan<IFactor<? super Double, IDiscretizedIntervalDomain<? extends Double>>> {

    //TODO put as optional fix the code
    @Input
    private Data<RConnectionInfo> rConnectionInfo = new Data("RConnectionInfo", RConnectionInfo.class);
    private int samplingNumber;
    private final static String RVariableName = "sa";

    public RFastMethod(int samplingNumber) {
        this.samplingNumber = samplingNumber;
    }
  
    public int getSamplingNumber() {
        return samplingNumber;
    }

    public void setSamplingNumber(int samplingNumber) {
        this.samplingNumber = samplingNumber;
    }

    @Override
    public IExploredPlan build(IContext context) {
        // this is the command that will be sent to R
        StringBuffer command = new StringBuffer(30);

        // we load the library
        command.append("library(\"sensitivity\")\n");
        // we call the fast method, and store the result in a variable
        command.append(RVariableName);
        command.append(" <- fast( factors = ");
        command.append(getFactors().size());
        command.append(", n = ");
        command.append(samplingNumber);
        // q are the domain method of the factors
        StringBuffer q = new StringBuffer();
        // and qarg are the arguments of these methods
        StringBuffer qArg = new StringBuffer();
        for (IFactor<?,?> factor : getFactors()) {
            if (factor.getDomain() instanceof RFunctionDomain) {
                q.append("\"");
                q.append(((RFunctionDomain) factor.getDomain()).getFunctionName());
                q.append("\",");
                qArg.append("list(");
                for (String arg : ((RFunctionDomain) factor.getDomain()).getArgs()) {
                    qArg.append(arg);
                    qArg.append(",");
                }
                qArg.deleteCharAt(qArg.length() - 1);
                qArg.append("),");
            }
        }
        q.deleteCharAt(q.length() - 1);
        qArg.deleteCharAt(qArg.length() - 1);
        command.append(", q = c(");
        command.append(q);
        command.append("), q.arg = list(");
        command.append(qArg);
        command.append("))\n");
        // finally, we request the factors value stored in our variable
        command.append(RVariableName);
        command.append("[[\"x\"]]");

        List<IFactorValues> listOfListOfValues = new ArrayList<IFactorValues>();


        try {
            REXP result = RConnectionManager.getConnection((RConnectionInfo) context.getGlobalValue(rConnectionInfo.getPrototype())).eval(command.toString());
            // the command has been evaluated, now we retrieve the result
            REXPVector vector = (REXPVector) result;
            RList matrix = vector.asList();

            for (int i = 0; i < computeSize(context); i++) {
                //values = new ArrayList<Object>(factors.size());
                FactorsValues factorValue = new FactorsValues();
                int j = 0;
                for (IFactor<? super Double, IDiscretizedIntervalDomain<? extends Double>> factor: getFactors()) {
                    factorValue.setValue(factor.getPrototype(), new Double(matrix.at(j).asDoubles()[i]));
                    j++;
                }

                listOfListOfValues.add(factorValue);
            }
        } catch (RserveException e) {
            e.printStackTrace();
        } catch (REXPMismatchException e) {
            e.printStackTrace();
        }
        int a = 2;

        return new ExploredPlan(listOfListOfValues);
    }

    protected int computeSize(IContext context) {
        return getFactors().size() * samplingNumber;
    }

    

}
