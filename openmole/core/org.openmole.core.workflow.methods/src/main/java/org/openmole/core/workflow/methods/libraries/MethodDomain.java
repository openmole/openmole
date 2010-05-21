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

import java.util.Iterator;
import java.util.List;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.core.workflow.methods.libraries.Library;
import org.openmole.core.workflow.methods.libraries.MethodDeclaration;
import org.openmole.core.workflow.methods.libraries.MethodInstance;
import org.openmole.core.workflow.methods.libraries.Platform;
import org.openmole.core.workflow.model.domain.IDomain;
import org.openmole.core.workflow.model.job.IContext;

/**
 * Creates a domain based upon a library method call.
 * This method is supposed to return a List of objects.
 * @param <T> The type of objects of the domain
 */
public class MethodDomain<T> implements IDomain<T> {

    private Platform platform;
    private Library library;
    private MethodDeclaration methodDeclaration;
    private MethodInstance method;

    public MethodDomain(Platform platform, Library library, MethodDeclaration methodDeclaration) throws InternalProcessingError {
        this.setMethod(platform, library, methodDeclaration);
    }

    public Platform getPlatform() {
        return platform;
    }

    public Library getLibrary() {
        return library;
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public MethodInstance getMethodInstance() {
        return method;
    }

    public void setMethod(Platform platform, Library library, MethodDeclaration methodDeclaration) throws InternalProcessingError {
        this.platform = platform;
        this.library = library;
        this.methodDeclaration = methodDeclaration;
        this.method = new MethodInstance(library, methodDeclaration);
    }

    public Object getParameterValue(String parameterName) {
        return method.getParameterValue(parameterName);
    }

    public Object setParameterValue(String parameterName, Object paramValue) {
        return method.setParameterValue(parameterName, paramValue);
    }

    /*@Override
    public int getSize() {
        return -1;
    }
*/
    protected List<T> expandValues(IContext context) {
        return (List<T>) platform.invoke(null, method);
    }

    @Override
    public Iterator<T> iterator(IContext context) throws UserBadDataError, InternalProcessingError {
        return expandValues(context).iterator();
    }

}
