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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
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
import org.openmole.plugin.resource.virtual.internal.Activator;
import static org.openmole.commons.tools.io.Network.*;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class VirtualMachineResource extends ComposedResource {

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
        File vmImage = systemResource.getDeployedFile();
        if (!vmImage.isFile()) {
            throw new UserBadDataError("Image " + vmImage.getAbsolutePath() + " doesn't exist or is not a file.");
        }

        class VirtualMachineConnector implements IConnectable {

            IVirtualMachine virtualMachine;

            @Override
            public void connect(int port) throws IOException, InternalProcessingError {
                File qemuDir = getQEmuDir();
                CommandLine commandLine = CommandLine.parse(new File(qemuDir, Executable).getAbsolutePath() + " -m " + memory + " -smp " + vcore + " -nographic -hda " + systemResource.getDeployedFile().getAbsolutePath() + " -L " + qemuDir.getAbsolutePath() + " -monitor null -serial none -redir tcp:" + port + "::22");

                Process process = commandLauncher().exec(commandLine, new HashMap());
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

        return connector.virtualMachine;
    }

    @Cachable
    private CommandLauncher commandLauncher() {
        return CommandLauncherFactory.createVMLauncher();
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
        final File qemuDir = Activator.getWorkspace().newTmpDir();
        final String qemuJarPath;

        if (os.toLowerCase().contains("linux")) {
            qemuJarPath = "/qemu_linux/";
        } else if (os.toLowerCase().contains("windows")) {
            qemuJarPath = "/qemu_windows/";
        } else {
            throw new InternalProcessingError("Unsuported OS " + os);
        }
        
        File qemu = new File(qemuDir, Executable);
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(qemu));
        try {
            FileUtil.copy(this.getClass().getClassLoader().getResource(qemuJarPath + Executable).openStream(), outputStream);
            qemu.setExecutable(true);
        } finally {
            outputStream.close();
        }

        for (String f : CommonFiles) {
            File dest = new File(qemuDir, f);
            outputStream = new BufferedOutputStream(new FileOutputStream(dest));

            try {
                FileUtil.copy(this.getClass().getClassLoader().getResource(f).openStream(), outputStream);
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
