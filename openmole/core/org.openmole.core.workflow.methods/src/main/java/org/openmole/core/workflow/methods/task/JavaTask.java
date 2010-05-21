/*
 *
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
package org.openmole.core.workflow.methods.task;

import java.lang.reflect.Method;

import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.core.workflow.implementation.task.Task;
import org.openmole.core.workflow.methods.editors.JavaCodeEditor;
import org.openmole.core.workflow.methods.editors.JavaCodeEditor.Arg;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.core.workflow.model.resource.ILocalFileCache;


/* Change to use new context to code architecture */
public class JavaTask extends Task {

    private JavaCodeEditor editor = new JavaCodeEditor();

    public JavaTask(String name) throws UserBadDataError, InternalProcessingError {
        super(name);
  /*      editor.add("fr.cemagref.simexplorer.ide.core.data.ComplexData");
        editor.add("fr.cemagref.simexplorer.ide.core.data.DataSequence");
        editor.add("fr.cemagref.simexplorer.ide.mexico.data.factors.ExperimentalFactorsValues");
        editor.add("fr.cemagref.simexplorer.ide.core.data.Variable");
        editor.add("fr.cemagref.simexplorer.ide.core.data.Constant");
        editor.add("fr.cemagref.simexplorer.ide.core.IDEModularApplication");
        editor.add("fr.cemagref.simexplorer.ide.core.data.Constants");
        editor.add("fr.cemagref.simexplorer.ide.core.data.Variables");
        editor.add("fr.cemagref.simexplorer.ide.core.data.GlobalOutputData");*/
        editor.add("java.util.List");
    }

    @Override
    protected void process(IContext context, IExecutionContext executionContext, IProgress progress) throws UserBadDataError, InternalProcessingError {
        Method method = editor.compileMethod(new Arg(IContext.class, "context"));
        if (method != null) {
            editor.callMethod(method, context);
        }
    }

    public void setCode(String code) {
        editor.setCode(code);
    }
    
}
