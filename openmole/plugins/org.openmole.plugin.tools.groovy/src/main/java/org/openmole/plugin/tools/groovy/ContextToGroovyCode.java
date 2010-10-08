/*
 *  Copyright (C) 2010 Romain Reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.tools.groovy;


import groovy.lang.Binding;
import org.openmole.core.implementation.tools.GroovyShellProxyAdapter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.implementation.execution.Progress;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IPrototype;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

import org.openmole.core.model.job.IContext;
import org.openmole.commons.tools.groovy.GroovyProxyPool;
import org.openmole.commons.tools.groovy.IGroovyProxy;
import org.openmole.commons.aspect.caching.Cachable;
import org.openmole.commons.aspect.caching.ChangeState;
import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.execution.IProgress;
import org.openmole.plugin.tools.code.FileSourceCode;
import org.openmole.plugin.tools.code.IContextToCode;
import org.openmole.plugin.tools.code.ISourceCode;
import org.openmole.plugin.tools.code.StringSourceCode;

public class ContextToGroovyCode implements IContextToCode {

    final static public IPrototype<IProgress> progressVar = new Prototype<IProgress>("progress", IProgress.class);

    private List<String> imports = new LinkedList<String>();
    private String code;

    @ChangeState
    @Override
    public void setCode(ISourceCode codeSource) throws InternalProcessingError, UserBadDataError {
        code = codeSource.getCode();
    }

    @Override
    public String getCode() {
        return code;
    }

    public void setCode(String code) throws UserBadDataError, InternalProcessingError {
        setCode(new StringSourceCode(code));
    }

    public void setCodeFile(String url) throws UserBadDataError, InternalProcessingError {
        setCode(new FileSourceCode(url));
    }


    private String getSource() throws InternalProcessingError, UserBadDataError {
        StringBuilder build = new StringBuilder();
        build.append(getImportString());
        build.append(getCode());
        return build.toString();
    }

    @Cachable
    private GroovyProxyPool getEditorPool() {
        return new GroovyProxyPool();
    }

    @Override
    public Object execute(IContext global, IContext context) throws UserBadDataError, InternalProcessingError {
        return execute(global, context,  Progress.DUMMY, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }

    @Override
    public Object execute(IContext global, IContext context, Iterable<IVariable> tmpVariables) throws UserBadDataError, InternalProcessingError {
        return execute(global, context, tmpVariables, Progress.DUMMY, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }

    /*public Object execute(IContext context, Iterable<File> libs) throws UserBadDataError, InternalProcessingError {
        return execute(context, Progress.DUMMY, Collections.EMPTY_LIST, libs);
    }*/
    @Override
    public Object execute(IContext global, IContext context, Iterable<IVariable> tmpVariables, Iterable<File> libs) throws UserBadDataError, InternalProcessingError {
        return execute(global, context, tmpVariables, Progress.DUMMY, Collections.EMPTY_LIST, libs);
    }

    @Override
    public Object execute(IContext global, IContext context, IProgress progress, Iterable<IData<?>> output, Iterable<File> libs) throws UserBadDataError, InternalProcessingError {
        return execute(global, context, Collections.EMPTY_LIST, progress, output, libs);
    }

    @Override
    public Object execute(IContext global, IContext context, Iterable<IVariable> tmpVariables, IProgress progress, Iterable<IData<?>> output, Iterable<File> libs) throws UserBadDataError, InternalProcessingError {
        GroovyProxyPool pool = getEditorPool();
        IGroovyProxy groovyProxy = pool.borrowObject();

        GroovyShellProxyAdapter editor = new GroovyShellProxyAdapter(groovyProxy);

        try {
            if (!editor.isScriptCompiled()) {
                editor.compile(getSource(), libs);
            }

            Binding binding = GroovyShellProxyAdapter.fromContextToBinding(global, context);

            for(IVariable variable: tmpVariables) {
                binding.setVariable(variable.getPrototype().getName(), variable.getValue());
            }

            binding.setVariable(progressVar.getName(), progress);
            
            Object ret;
            try {
                ret = editor.execute(binding);
            } catch (Throwable t) {
                throw new UserBadDataError(t, "Script Error !\nThe script was :\n" + getSource() + "\nError message was:\n" + t.getMessage());
            }

            fetchVariables(context, output, binding);
            return ret;
        } finally {
            pool.returnObject(groovyProxy);
        }

    }

    @ChangeState
    public void addImport(String pack) {
        imports.add(pack);
    }

    public final void fetchVariables(IContext context, Iterable<IData<?>> output, Binding binding) throws UserBadDataError, InternalProcessingError {
        Map variables = binding.getVariables();
        for (IData<?> data : output) {
            IPrototype out = data.getPrototype();

            Object val = variables.get(out.getName());
            if (val != null) {
                if (out.getType().isAssignableFrom(val.getClass())) {
                    context.setValue(out, val);
                } else {
                    throw new InternalProcessingError("Variable " + out.getName() + " of type " + val.getClass().getName() + " has been found at the end of the execution of the groovy code but type doesn't match : " + out.getType().getName() + ".");
                }
            }
        }
    }

    @SoftCachable
    public String getImportString() {
        StringBuilder build = new StringBuilder();

        for (String imp : imports) {
            build.append("import ");
            build.append(imp);
            build.append("\n");
        }

        return build.toString();
    }


}


