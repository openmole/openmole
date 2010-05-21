/*
 *  Copyright (c) 2008, Cemagref
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
package org.openmole.core.workflow.methods.libraries;

import org.openmole.core.workflow.methods.r.RConnectionInfo;
import org.openmole.core.workflow.methods.r.RConnectionManager;
import org.openmole.core.workflow.model.job.IContext;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RserveException;

public class PlatformR extends Platform {

    @Override
    public void load(Library library) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getInvokeCode(MethodInstance method) {
        StringBuilder rcommand = new StringBuilder(60);
        rcommand.append("library(").append(method.getLibraryName()).append(")\n");
        rcommand.append(method.getMethodName()).append("(");
        for (int i = 0; i < method.getParametersNames().length; i++) {
            if (i != 0) {
                rcommand.append(", ");
            }
            rcommand.append(method.getParametersNames()[i]).append("=").append(method.getParametersValues()[i]);
        }
        // TODO how to retrieve the whole objects
        rcommand.append(")[\"x\"]\n");
        return rcommand.toString();
    }

    @Override
    public Object invoke(IContext context, MethodInstance method) {
        try {
            REXP result = RConnectionManager.getConnection((RConnectionInfo) context.getLocalValue("RConnectionInfo")).eval(getInvokeCode(method));
            return result;
        } catch (RserveException ex) {
        	Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void close(Library library) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void install() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
