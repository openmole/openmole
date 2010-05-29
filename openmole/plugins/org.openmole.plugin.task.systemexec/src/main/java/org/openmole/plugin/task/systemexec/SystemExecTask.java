/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.task.systemexec;

import java.io.File;
import java.io.IOException;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.job.IContext;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.openmole.core.workflow.implementation.data.Prototype;
import org.openmole.core.workflow.implementation.data.Variable;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.data.IVariable;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.plugin.task.external.ExternalTask;
import org.openmole.plugin.task.systemexec.internal.Activator;

import static org.openmole.core.workflow.implementation.tools.VariableExpansion.*;
public class SystemExecTask extends ExternalTask {

    final public static IPrototype<File> PWD = new Prototype<File>("PWD", File.class);

    final String cmd;
    final Prototype<Integer> returnValue;

    public SystemExecTask(String name, String cmd) throws UserBadDataError,
            InternalProcessingError {
        this(name, cmd, null);
    }

    public SystemExecTask(String name, String cmd, Prototype<Integer> returnValue) throws UserBadDataError,
            InternalProcessingError {
        super(name);
        this.cmd = cmd;
        this.returnValue = returnValue;
        if(returnValue != null) addOutput(returnValue);
    }

    public String getCmd() {
        return cmd;
    }

    public Prototype<Integer> getReturnValue() {
        return returnValue;
    }



    @Override
    protected void process(IContext context, IExecutionContext executionContext, IProgress progress)
            throws UserBadDataError, InternalProcessingError {
        try {
            File tmpDir = Activator.getWorkspace().newTmpDir("systemExecTask");

            prepareInputFiles(context, progress, tmpDir);
            
            List<IVariable> tmpVariables = new LinkedList<IVariable>();
            tmpVariables.add(new Variable(PWD, tmpDir));
            CommandLine commandLine = CommandLine.parse(expandData(context,tmpVariables, cmd));

            DefaultExecutor executor = new DefaultExecutor();
            executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
            executor.setWorkingDirectory(tmpDir);


            try {
                Integer ret = executor.execute(commandLine);
                if(returnValue != null) context.setValue(returnValue, ret);
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            } 

            fetchOutputFiles(context, progress, tmpDir);

        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }
    }
}
