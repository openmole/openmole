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

package org.openmole.plugin.task.systemexectask;

import java.io.File;
import java.io.IOException;

import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.job.IContext;

import com.developpez.adiguba.shell.ProcessConsumer;
import com.developpez.adiguba.shell.Shell;
import java.util.LinkedList;
import java.util.List;
import org.openmole.core.workflow.implementation.data.Prototype;
import org.openmole.core.workflow.implementation.data.Variable;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.data.IVariable;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.plugin.task.externaltask.ExternalTask;
import org.openmole.plugin.task.systemexectask.internal.Activator;

import static org.openmole.core.workflow.implementation.tools.VariableExpansion.*;
public class SystemExecTask extends ExternalTask {

    final static IPrototype<File> PWD = new Prototype<File>("PWD", File.class);

    String cmd;

    public SystemExecTask(String name) throws UserBadDataError, InternalProcessingError {
        super(name);
    }
    
    public SystemExecTask(String name, String cmd) throws UserBadDataError,
            InternalProcessingError {
        super(name);
        this.cmd = cmd;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Override
    protected void process(IContext context, IExecutionContext executionContext, IProgress progress)
            throws UserBadDataError, InternalProcessingError {
        try {
            File tmpDir = Activator.getWorkspace().newTmpDir("systemExecTask");

            prepareInputFiles(context, progress, tmpDir);

            Shell shell = new Shell();
            shell.setDirectory(tmpDir);

            List<IVariable> tmpVariables = new LinkedList<IVariable>();
            tmpVariables.add(new Variable(PWD, tmpDir));

            ProcessConsumer c;
            c = shell.command(expandData(context,tmpVariables, cmd));

            try {
                c.consume();
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }

            fetchOutputFiles(context, progress, tmpDir);

        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }
    }
}
