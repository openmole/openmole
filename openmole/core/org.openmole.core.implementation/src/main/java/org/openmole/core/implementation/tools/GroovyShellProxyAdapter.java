/*
 *  Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.tools;

import groovy.lang.Binding;
import java.io.File;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.groovy.IGroovyProxy;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.model.job.IContext;
import org.openmole.misc.workspace.IWorkspace;
import org.openmole.core.implementation.internal.Activator;
/**
 *
 * @author reuillon
 */
public class GroovyShellProxyAdapter implements IGroovyProxy {

    final static public Prototype<IContext> globalContextVar = new Prototype<IContext>("global", IContext.class);
    final static public Prototype<IContext> contextVar = new Prototype<IContext>("context", IContext.class);
    final static public Prototype<IWorkspace> workspaceVar = new Prototype<IWorkspace>("workspace", IWorkspace.class);

    IGroovyProxy groovyShellProxy;

    public GroovyShellProxyAdapter(IGroovyProxy groovyShellProxy) {
        this.groovyShellProxy = groovyShellProxy;
    }

    public static Binding fromContextToBinding(IContext global, IContext context) {
         Binding binding = new Binding();

         binding.setVariable(globalContextVar.getName(), global);
         binding.setVariable(contextVar.getName(), context);
         binding.setVariable(workspaceVar.getName(), Activator.getWorkspace());

        for (IVariable in : context.getVariables().values()) {
           binding.setVariable(in.getPrototype().getName(), in.getValue());
        }

        return binding;
    }


    @Override
    public boolean isScriptCompiled() {
        return groovyShellProxy.isScriptCompiled();
    }


    public IGroovyProxy getGroovyShellProxy() {
        return groovyShellProxy;
    }

    @Override
    public void compile(String code, Iterable<File> jars) throws InternalProcessingError, UserBadDataError {
         groovyShellProxy.compile(code,jars);
    }

    @Override
    public Object execute(Binding binding) {
        return groovyShellProxy.execute(binding);
    }
    public Object execute(IContext global, IContext binding) {
        return execute(fromContextToBinding(global, binding));
    }
}
