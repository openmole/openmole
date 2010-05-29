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
package org.openmole.plugin.task.netlogo;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.nlogo.api.CompilerException;
import org.nlogo.api.LogoException;
import org.nlogo.headless.HeadlessWorkspace;
import org.openmole.core.workflow.implementation.tools.VariableExpansion;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.plugin.task.external.ExternalTask;
import org.openmole.plugin.task.netlogo.internal.Activator;

/**
 *
 * @author reuillon
 */
public class NetLogoTask extends ExternalTask {

    Iterable<String> launchingCommands;

    List<Duo<IPrototype, String>> inputBinding = new LinkedList<Duo<IPrototype, String>>();
    List<Duo<String, IPrototype>> outputBinding = new LinkedList<Duo<String, IPrototype>>();

    String relativeScriptPath;

    public NetLogoTask(String name,
            File workspace,
            String sriptName,
            Iterable<String> launchingCommands) throws UserBadDataError, InternalProcessingError {
        super(name);
        this.relativeScriptPath = workspace.getName() + "/" + sriptName;
        addInFile(workspace);
        this.launchingCommands = launchingCommands;
    }

    public NetLogoTask(String name,
            String workspace,
            String sriptName,
            Iterable<String> launchingCommands) throws UserBadDataError, InternalProcessingError {
            this(name, new File(workspace), sriptName, launchingCommands);
    }


    @Override
    protected void process(IContext context, IExecutionContext executionContext, IProgress progress) throws UserBadDataError, InternalProcessingError, InterruptedException {
        try {
            File tmpDir = Activator.getWorkspace().newTmpDir("netLogoTask");
            prepareInputFiles(context, progress, tmpDir);
            File script = new File(tmpDir, relativeScriptPath);
            HeadlessWorkspace workspace = HeadlessWorkspace.newInstance();
            try {

                workspace.open(script.getAbsolutePath());
                
                for (Duo<IPrototype, String> inBinding : getInputBinding()) {
                    Object val = context.getLocalValue(inBinding.getLeft());
                    workspace.command("set " + inBinding.getRight() + " " + val.toString());
                }

                for (String cmd : launchingCommands) {
                    workspace.command(VariableExpansion.getInstance().expandData(context, cmd));
                }

                for (Duo<String, IPrototype> outBinding : getOutputBinding()) {
                    context.setValue(outBinding.getRight(), workspace.report(outBinding.getLeft()));
                }

                fetchOutputFiles(context, progress, tmpDir);
            } catch (CompilerException ex) {
                throw new UserBadDataError(ex);
            } catch (LogoException ex) {
                throw new InternalProcessingError(ex);
            } finally {
                workspace.dispose();
            }
        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }
    }

/*    @Override
    public void addInput(IPrototype prototype) {
        addInput(prototype, prototype.getName());
    }
*/
  /*  @Override
    public void addOutput(IPrototype prototype) {
        addOutput(prototype.getName(), prototype);
    }
*/
    public void addInput(IPrototype prototype, String binding) {
        inputBinding.add(new Duo<IPrototype, String>(prototype, binding));
        super.addInput(prototype);
    }

    public void addOutput(String binding, IPrototype prototype) {
        outputBinding.add(new Duo<String, IPrototype>(binding, prototype));
        super.addOutput(prototype);
    }

    private List<Duo<IPrototype, String>> getInputBinding() {
        return inputBinding;
    }

    private List<Duo<String, IPrototype>> getOutputBinding() {
        return outputBinding;
    }
}
