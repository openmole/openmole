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

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.exec.launcher.CommandLauncher;
import org.apache.commons.exec.launcher.CommandLauncherFactory;
import org.openmole.commons.aspect.caching.Cachable;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.io.FileUtil;
import org.openmole.core.implementation.resource.ComposedResource;
import org.openmole.core.implementation.resource.FileResource;
import org.openmole.core.model.task.annotations.Resource;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.misc.workspace.ConfigurationLocation;
import static org.openmole.plugin.resource.virtual.internal.Activator.*;
import static org.openmole.commons.tools.io.Network.*;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class VirtualMachineResource extends ComposedResource {

    final static ConfigurationLocation VMBootTime = new ConfigurationLocation(VirtualMachineResource.class.getSimpleName(), "VMBootTime");

    static {
        workspace().addToConfigurations(VMBootTime, "PT5M");
    }

    final static String[] CommonFiles = {"bios.bin"};
    final static String Executable = "qemu";
    @Resource
    final FileResource systemResource;
    final String user;
    final String password;
    final int memory;
    final int vcore;

    public VirtualMachineResource(String system, String user, String password) {
        this(new File(system), user, password);
    }

    public VirtualMachineResource(File system, String user, String password) {
        this(system, user, password, 256);
    }

    public VirtualMachineResource(String system, String user, String password, int memory) {
        this(new File(system), user, password, memory);
    }

    public VirtualMachineResource(File system, String user, String password, int memory) {
        this(system, user, password, memory, 1);
    }

    public VirtualMachineResource(String system, String user, String password, int memory, int vcore) {
        this(new File(system), user, password, memory, vcore);
    }

    public VirtualMachineResource(File system, String user, String password, int memory, int vcore) {
        this.systemResource = new FileResource(system);
        this.user = user;
        this.password = password;
        this.memory = memory;
        this.vcore = vcore;
    }

    public IVirtualMachine launchAVirtualMachine() throws UserBadDataError, InternalProcessingError {
        if (!systemResource.getDeployedFile().isFile()) {
            throw new UserBadDataError("Image " + systemResource.getDeployedFile().getAbsolutePath() + " doesn't exist or is not a file.");
        }

        final File vmImage;
        try {
            vmImage = workspace().newTmpFile();
            FileUtil.copy(systemResource.getDeployedFile(), vmImage);
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }

        class VirtualMachineConnector implements IConnectable {

            IVirtualMachine virtualMachine;

            @Override
            public void connect(int port) throws IOException, InternalProcessingError {
                File qemuDir = getQEmuDir();  

                CommandLine commandLine = new CommandLine(new File(qemuDir, Executable));
                commandLine.addArguments("-m " + memory + " -smp " + vcore +  " -redir tcp:" + port + "::22 -nographic -hda ");
                commandLine.addArgument(systemResource.getDeployedFile().getAbsolutePath());
                commandLine.addArguments("-L");
                commandLine.addArgument(qemuDir.getAbsolutePath());
                commandLine.addArguments("-monitor null -serial none");

                Process process = Runtime.getRuntime().exec(commandLine.toString());
                //Prevent qemu network from working on Windoze?
                //Process process = commandLauncher().exec(commandLine, new HashMap());
                processDestroyer().add(process);

                virtualMachine = new VirtualMachine("localhost", port, process);
            }
        }

        VirtualMachineConnector connector = new VirtualMachineConnector();

        try {
            ConnectToFreePort(connector);
        } catch (Exception e) {
            throw new InternalProcessingError(e);
        }
        final IVirtualMachine ret = connector.virtualMachine;
        //First connection
        final Connection connection = new Connection(ret.host(), ret.port());

        Long timeOut = workspace().getPreferenceAsDurationInMs(VMBootTime);

        try {
            connection.connect(null, timeOut.intValue(), timeOut.intValue());
        } catch (IOException ex) {
            throw new InternalProcessingError(ex, "Connection to the VM timeout the boot taked too long.");
        }
        connection.close();

    //Not supossed to fail but sometimes it does
       /*executorService().getExecutorService(ExecutorType.OWN).submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                connection.connect(null, ,);
                return null;
            }
      });*/


        return connector.virtualMachine;
    }
    
    @Cachable
    private ShutdownHookProcessDestroyer processDestroyer() {
        return new ShutdownHookProcessDestroyer();
    }

    @Cachable
    IVirtualMachinePool getVirtualMachinePool() {
        return new VirtualMachinePool(this);
    }

    @Cachable
    public IVirtualMachinePool getVirtualMachineShared() {
        return new VirtualMachineShared(this);
    }

    @Cachable
    private File getQEmuDir() throws IOException, InternalProcessingError {
        final String os = System.getProperty("os.name");
        final File qemuDir = workspace().newTmpDir();
        final String qemuJarPath;
        final String[] toCopy;

        if (os.toLowerCase().contains("linux")) {
            qemuJarPath = "/qemu_linux/";
            toCopy = new String[]{Executable};
        } else if (os.toLowerCase().contains("windows")) {
            qemuJarPath = "/qemu_windows/";
            toCopy = new String[]{Executable + ".exe", "SDL.dll"};
        } else {
            throw new InternalProcessingError("Unsuported OS " + os);
        }

        for (String f : toCopy) {
            File qemu = new File(qemuDir, f);
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(qemu));
            try {
                FileUtil.copy(this.getClass().getClassLoader().getResource(qemuJarPath + f).openStream(), outputStream);
                qemu.setExecutable(true);
                qemu.deleteOnExit();
            } finally {
                outputStream.close();
            }
        }
        for (String f : CommonFiles) {
            File dest = new File(qemuDir, f);
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(dest));

            try {
                FileUtil.copy(this.getClass().getClassLoader().getResource(f).openStream(), outputStream);
                dest.deleteOnExit();
            } finally {
                outputStream.close();
            }
        }


        return qemuDir;
    }

    public String password() {
        return password;
    }

    public String user() {
        return user;
    }
}
