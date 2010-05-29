/*
 *  Copyright (C) 2010 Cemagref
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
package org.simexplorer.openmole.plugin.task.sensitivity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.nuiton.j2r.RException;
import org.nuiton.j2r.types.RDataFrame;
import org.openmole.core.implementation.plan.ExploredPlan;
import org.openmole.core.implementation.plan.FactorsValues;
import org.openmole.core.implementation.plan.Plan;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.plan.IExploredPlan;
import org.openmole.core.model.plan.IFactor;
import org.openmole.core.model.plan.IFactorValues;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Nicolas Dumoulin <nicolas.dumoulin@cemagref.fr>
 */
public class FastPlan extends Plan<IFactor<Object, ?>> {

    private static final Logger LOGGER = Logger.getLogger(FastPlan.class.getName());
    private int samplingNumber;

    public FastPlan(int samplingNumber) {
        this.samplingNumber = samplingNumber;
    }

    @Override
    public IExploredPlan build(IContext context) throws InternalProcessingError, UserBadDataError, InterruptedException {
        List<IFactorValues> listOfListOfValues = new ArrayList<IFactorValues>();
        StringBuilder command = new StringBuilder(50);
        try {
            // we load the library
            R.voidEval("library('sensitivity')");
            // we call the fast method, and store the result in a variable
            command.append(TellTask.RVariableName);
            command.append(" <- fast99( factors = ");
            command.append(getFactors().size());
            command.append(", n = ");
            command.append(samplingNumber);
            // q are the domain method of the factors
            StringBuffer q = new StringBuffer();
            // and qarg are the arguments of these methods
            StringBuffer qArg = new StringBuffer();
            for (IFactor<?, ?> factor : getFactors()) {
                if (factor.getDomain() instanceof RFunctionDomain) {
                    q.append("'");
                    q.append(((RFunctionDomain) factor.getDomain()).getFunction());
                    q.append("',");
                    qArg.append("list(");
                    for (String arg : ((RFunctionDomain) factor.getDomain()).getArguments()) {
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
            command.append("))");
            R.voidEval(command.toString());
            // then, we request the factors value stored in our variable
            command = new StringBuilder();
            command.append(TellTask.RVariableName);
            command.append("[['X']]");
            RDataFrame result = (RDataFrame) R.eval(command.toString());
            for (int i = 0; i < samplingNumber; i++) {
                FactorsValues factorValues = new FactorsValues();
                for (int j = 0; j < getFactors().size(); j++) {
                    IFactor<Object, ?> factor = getFactors().get(j);
                    factorValues.setValue(factor.getPrototype(), result.getData().get(j).get(i + samplingNumber * j));
                }
                listOfListOfValues.add(factorValues);
            }
            return new ExploredPlan(listOfListOfValues);
        } catch (RException ex) {
            throw new InternalProcessingError(ex, "Problem during the communication with R, the command was:\n" + command);
        }
    }
}
