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

import org.openmole.core.workflow.model.job.IContext;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.misc.exception.FatalError;

public class PlatformJava extends Platform {

    @Override
    public void load(Library library) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getInvokeCode(MethodInstance method) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // TODO for instance, only static methods are handled
    @Override
    public Object invoke(IContext context, MethodInstance method) {
        throw new FatalError("Not implemented yet");

       /* try {
            StringBuffer className = new StringBuffer(method.getMetadata("package"));
            if (className.length() > 0) {
                className.append(".");
            }
            className.append(method.getMetadata("class"));
            Class methodClass;
            methodClass = Class.forName(className.toString());
            java.lang.reflect.Method reflectMethod = methodClass.getDeclaredMethod(method.getMethodName(), method.getParametersTypes());
            return reflectMethod.invoke(null, method.getParametersValues());
        } catch (Exception ex) {
        	Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex);
        }
        return null;*/
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
