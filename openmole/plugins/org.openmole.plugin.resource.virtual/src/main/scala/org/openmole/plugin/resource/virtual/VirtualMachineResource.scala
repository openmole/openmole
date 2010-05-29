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

package org.openmole.plugin.resource.virtual

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.HashMap
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.ProcessDestroyer
import org.apache.commons.exec.ShutdownHookProcessDestroyer
import org.apache.commons.exec.launcher.CommandLauncher
import org.apache.commons.exec.launcher.CommandLauncherFactory
import org.openmole.core.implementation.resource.ComposedResource
import org.openmole.core.implementation.resource.FileResource
import org.openmole.core.model.task.annotations.Resource
import org.openmole.commons.aspect.caching.Cachable
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.commons.tools.io.FastCopy
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.plugin.resource.virtual.internal.Activator

import org.openmole.commons.tools.io.Network._


class VirtualMachineResource(system: File, user: String, password: String) extends ComposedResource {

    object Files {
      def list = List("qemu", "bios.bin")
    }

    object Configuration {
      def VirtualMachineBootTimeOut = {
        val ret = new ConfigurationLocation(classOf[VirtualMachine].getSimpleName(), "VirtualMachineBootTimeOut")
        Activator.getWorkspace().addToConfigurations(ret, "PT5M")
        ret
      }
    }

    @Resource
    val systemResource: FileResource = new FileResource(system)


    def this(system: String, user: String, password: String) {
        this(new File(system), user, password)
    }

    def launchAVirtualMachine: IVirtualMachine = {

        class VirtualMachineConnector extends IConnectable {
            var virtualMachine: IVirtualMachine = null

            override def connect(port: Int) = {
               val qemuDir = getQEmuDir
               val commandLine = CommandLine.parse(new File(qemuDir, "qemu").getAbsolutePath() + " -nographic -hda " + systemResource.getDeployedFile().getAbsolutePath() + " -L " + qemuDir.getAbsolutePath() + " -redir tcp:" + port + "::22")

               val process = commandLauncher.exec(commandLine, new HashMap())
               processDestroyer.add(process)

               virtualMachine = new VirtualMachine("localhost", port, process)
            }
        }

        val connector = new VirtualMachineConnector

        try {
            ConnectToFreePort(connector)
        } catch {
          case e: Exception => throw new InternalProcessingError(e)
        }

        val virtualMachine = connector.virtualMachine
        val jsch = new JSch()

        try {
            val session = jsch.getSession(user, virtualMachine.host, virtualMachine.port)
            session.setPassword(password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect( Activator.getWorkspace().getPreferenceAsDurationInS(Configuration.VirtualMachineBootTimeOut).intValue )
            session.disconnect

        } catch {
          case ex: JSchException => throw new InternalProcessingError(ex)
        }

        connector.virtualMachine
    }

    @Cachable
    private def commandLauncher: CommandLauncher = {
        CommandLauncherFactory.createVMLauncher
    }

    @Cachable
    private def processDestroyer: ShutdownHookProcessDestroyer = {
        new ShutdownHookProcessDestroyer
    }

    @Cachable
    def getVirtualMachinePool: IVirtualMachinePool = {
        new VirtualMachinePool(this)
    }

    @Cachable
    def getVirtualMachineShared: IVirtualMachinePool = {
        new VirtualMachineShared(this)
    }

    @Cachable
    private def getQEmuDir: File = {
        val os = System.getProperty("os.name")
        val qemuDir = Activator.getWorkspace().newTmpDir()
        val qemuJarPath = 
          if(os.toLowerCase().contains("linux")) {
            "/qemu_linux/"
          } else {
            throw new InternalProcessingError("Unsuported OS " + os)
          }
        
        
        Files.list.foreach( f => {
            val dest = new File(qemuDir, f)
            val outputStream = new BufferedOutputStream(new FileOutputStream(dest))

            try {
                FastCopy.copy(this.getClass().getClassLoader().getResource(qemuJarPath + f).openStream(), outputStream)
            } finally {
                outputStream.close
            }
            dest.setExecutable(true)
        } )

        qemuDir
    }
}
