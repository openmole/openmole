/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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
package org.openmole.plugin.resource.virtual;

import ch.ethz.ssh2.Connection;
import java.io.BufferedOutputStream;
import java.io.File;
import org.openmole.commons.tools.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.openmole.commons.aspect.caching.Cachable;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.io.FileUtil;
import org.openmole.commons.tools.io.StringBuilderOutputStream;
import org.openmole.core.model.resource.IResource;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.plugin.resource.virtual.internal.Activator;
import static org.openmole.plugin.resource.virtual.internal.Activator.*;
import static org.openmole.commons.tools.io.Network.*;
import static org.openmole.plugin.tools.utils.ProcessUtils.*;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class VirtualMachineResource implements IResource {

    final static ConfigurationLocation VMBootTime = new ConfigurationLocation(VirtualMachineResource.class.getSimpleName(), "VMBootTime");

    static {
        workspace().addToConfigurations(VMBootTime, "PT5M");
    }
    final static String[] CommonFiles = {"bios.bin"};
    final static String Executable = "qemu";

    final File system;

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
        this.system = system;
        this.user = user;
        this.password = password;
        this.memory = memory;
        this.vcore = vcore;
    }

    public IVirtualMachine launchAVirtualMachine() throws UserBadDataError, InternalProcessingError, InterruptedException {
        if (!system.isFile()) {
            throw new UserBadDataError("Image " + system.getAbsolutePath() + " doesn't exist or is not a file.");
        }

        final File vmImage;
        try {
            vmImage = workspace().newFile();
            FileUtil.copy(system, vmImage);
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }

        class VirtualMachineConnector implements IConnectable {

            IVirtualMachine virtualMachine;

            @Override
            public void connect(int port) throws IOException, InternalProcessingError, InterruptedException {
                File qemuDir = getQEmuDir();

                CommandLine commandLine = new CommandLine(new File(qemuDir, Executable));
                commandLine.addArguments("-m " + memory + " -smp " + vcore + " -redir tcp:" + port + "::22 -nographic -hda ");
                commandLine.addArgument(system.getAbsolutePath());
                commandLine.addArguments("-L");
                commandLine.addArgument(qemuDir.getAbsolutePath());
                commandLine.addArguments("-monitor null -serial none");

                Process process = Runtime.getRuntime().exec(commandLine.toString());
                //Prevent qemu network from working on Windoze?
                //Process process = commandLauncher().exec(commandLine, new HashMap());
                ShutdownHookProcessDestroyer destroyer = processDestroyer();
                destroyer.add(process);

                virtualMachine = new VirtualMachine("localhost", port, process, destroyer, vmImage);
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
        //final Connection connection = new Connection(ret.host(), ret.port());

        final Long timeOut = workspace().getPreferenceAsDurationInMs(VMBootTime);

        Future connectionFuture = Activator.executorService().getExecutorService(ExecutorType.OWN).submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                while (true) {
                    try {
                        Socket socket = new Socket(ret.host(), ret.port());
                        socket.close();
                        Connection connection = new Connection(ret.host(), ret.port());
                        connection.connect(null, 0, 0);
                        connection.close();
                      //socket.close();
                        break;
                    } catch (IOException ex) {
                        Logger.getLogger(VirtualMachineResource.class.getName()).log(Level.WARNING, "Problem durring the connection, retrying...", ex);
                    }
                }
                return null;
            }
        });

        try {
            connectionFuture.get(timeOut, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            connector.virtualMachine.shutdown();
            throw e;
        } catch (ExecutionException ex) {
            connector.virtualMachine.shutdown();
            throw new InternalProcessingError(ex, "Connection to the VM has raised an error.");
        } catch (TimeoutException ex) {
            connectionFuture.cancel(true);
            connector.virtualMachine.shutdown();
            throw new InternalProcessingError(ex, "Connection to the VM timeout the boot taked too long.");
        }

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
    private File getQEmuDir() throws IOException, InternalProcessingError, InterruptedException {
        final String os = System.getProperty("os.name");

        final File qemuDir = workspace().newDir();
        final String qemuJarPath;
        final String[] toCopy;

        //Logger.getLogger(VirtualMachineResource.class.getSimpleName()).info(System.getProperty("os.name"));
        //Logger.getLogger(VirtualMachineResource.class.getSimpleName()).info(System.getProperty("os.version"));

        if (os.toLowerCase().contains("linux")) {
            Process process = Runtime.getRuntime().exec("uname -a");

            StringBuilder builder = new StringBuilder();
            executeProcess(process, new PrintStream(new StringBuilderOutputStream(builder)), System.err);
            String res = builder.toString();

            if (res.contains("x86_64")) {
                qemuJarPath = "/qemu_linux_64/";
            } else {
                qemuJarPath = "/qemu_linux_32/";
            }
            toCopy = new String[]{Executable};
        } else if (os.toLowerCase().contains("windows")) {
            qemuJarPath = "/qemu_windows/";
            toCopy = new String[]{Executable + ".exe", "SDL.dll"};
        } else if (os.toLowerCase().contains("mac")) {
            if (System.getProperty("os.version").contains("10.6")) {
                qemuJarPath = "/qemu_OSX-10.6/";
                toCopy = new String[]{Executable};
            } else {
                throw new InternalProcessingError("Unsuported OSX version " + System.getProperty("os.version"));
            }
        } else {
            throw new InternalProcessingError("Unsuported OS " + os);
        }

        for (String f : toCopy) {
            File qemu = new File(qemuDir, f);
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(qemu));
            try {
                FileUtil.copy(this.getClass().getClassLoader().getResource(qemuJarPath + f).openStream(), outputStream);
                qemu.setExecutable(true);
            } finally {
                outputStream.close();
            }
        }
        for (String f : CommonFiles) {
            File dest = new File(qemuDir, f);
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(dest));

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

    @Override
    public void deploy() throws InternalProcessingError, UserBadDataError {
        
    }
}
