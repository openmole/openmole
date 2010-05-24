/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.plugin.resource.virtual;


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.File;
import java.util.HashMap;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ProcessDestroyer;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.exec.launcher.CommandLauncher;
import org.apache.commons.exec.launcher.CommandLauncherFactory;
import org.openmole.core.workflow.implementation.resource.ComposedResource;
import org.openmole.core.workflow.implementation.resource.FileResource;
import org.openmole.core.workflow.model.task.annotations.Resource;
import org.openmole.misc.caching.Cachable;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.plugin.resource.virtual.internal.Activator;
import static org.openmole.misc.tools.io.Network.*;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class VirtualMachineResource extends ComposedResource {

    static ConfigurationLocation VirtualMachineBootTimeOut = new ConfigurationLocation(VirtualMachine.class.getSimpleName(), "VirtualMachineBootTimeOut");

    static {
        Activator.getWorkspace().addToConfigurations(VirtualMachineBootTimeOut, "PT5M");
    }


    @Resource
    final FileResource emulator;

    @Resource
    final FileResource system;

    final String user;

    final String password;

    public VirtualMachineResource(File emulator, File system, String user, String password) {
        this.emulator = new FileResource(emulator);
        this.system = new FileResource(system);
        this.user = user;
        this.password = password;
    }

    IVirtualMachine launchAVirtualMachine() throws InternalProcessingError, UserBadDataError {
       
        class VirtualMachineConnector implements IConnectable {
            IVirtualMachine virtualMachine;

            @Override
            public void connect(int port) throws Exception {
               CommandLine commandLine = CommandLine.parse(emulator.getDeployedFile().getAbsolutePath() + " -hda " + system.getDeployedFile().getAbsolutePath() + " tcp:" + port + "::22");
               
               Process process = getCommandLauncher().exec(commandLine, new HashMap());
               getProcessDestroyer().add(process);

               virtualMachine = new VirtualMachine("localhost", port, process);
            }

            public IVirtualMachine getVirtualMachine() {
                return virtualMachine;
            }
        }

        VirtualMachineConnector connector = new VirtualMachineConnector();

        try {
            ConnectToFreePort(connector);
        } catch (Exception e) {
            throw new InternalProcessingError(e);
        }
        IVirtualMachine virtualMachine = connector.getVirtualMachine();
        JSch jsch = new JSch();

        try {
            Session session = jsch.getSession(user, virtualMachine.getHost(), 22);
            session.setPassword(password);
            session.connect( Activator.getWorkspace().getPreferenceAsDurationInS(VirtualMachineBootTimeOut) );
            session.disconnect();
        } catch (JSchException ex) {
            throw new InternalProcessingError(ex);
        }


        return connector.getVirtualMachine();
    }

    @Cachable
    private CommandLauncher getCommandLauncher() {
        return CommandLauncherFactory.createVMLauncher();
    }

    @Cachable
    private ProcessDestroyer getProcessDestroyer() {
        return new ShutdownHookProcessDestroyer();
    }

    @Cachable
    public IVirtualMachinePool getVirtualMachinePool() {
        return new VirtualMachinePool(this);
    }
    
}
