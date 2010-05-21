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

import java.util.ArrayList;
import java.util.List;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.core.workflow.implementation.domain.FiniteDomain;
import org.openmole.core.workflow.model.job.IContext;

public class RFunctionDomain extends FiniteDomain<Double> {

    private String functionName;
    private String[] args;
    private RConnectionInfo info;
    private transient StringBuffer rcommand;

    public RFunctionDomain(RConnectionInfo info, String functionName, String... args) {
        this.info = info;
        this.functionName = functionName;
        this.args = args;
        buildRCommand();
    }

    public RFunctionDomain() {

    }

    private void buildRCommand() {
        rcommand = new StringBuffer(30);
        rcommand.append(functionName);
        rcommand.append("(");
        for (int i = 0 ; i < args.length ; i++) {
            if (i != 0) {
                rcommand.append(",");
            }
            rcommand.append(args[i]);
        }
        rcommand.append(")");
    }

 

    @Override
    public List<Double> computeValues(IContext context) throws InternalProcessingError {
        if (rcommand == null) {
            buildRCommand();
        }
        List<Double> values = new ArrayList<Double>();
        try {
            REXP result = RConnectionManager.getConnection(info).eval(rcommand.toString());
            for (double value : result.asDoubles()) {
                values.add(value);
            }
        } catch (RserveException e) {
           throw new InternalProcessingError(e);
        } catch (REXPMismatchException e) {
           throw new InternalProcessingError(e);
        }

        return values;

    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }



}
