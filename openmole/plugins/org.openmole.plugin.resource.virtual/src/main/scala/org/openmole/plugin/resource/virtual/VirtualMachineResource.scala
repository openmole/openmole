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
import org.openmole.commons.tools.io.FileUtil
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.plugin.resource.virtual.internal.Activator

import org.openmole.commons.tools.io.Network._
import ch.ethz.ssh2._

class VirtualMachineResource(system: File, val user: String, val password: String, memory: Int = 256, vcore: Int = 1) extends ComposedResource {

  object Files {
    def list = List("qemu", "bios.bin")
  }

  object Configuration {
    val VirtualMachineBootTimeOut = new ConfigurationLocation(classOf[VirtualMachine].getSimpleName(), "VirtualMachineBootTimeOut")
    Activator.getWorkspace().addToConfigurations(VirtualMachineBootTimeOut, "PT5M")
  }

  @Resource
  val systemResource: FileResource = new FileResource(system)

  def this(system: String, user: String, password: String) {
    this(new File(system), user, password)
  }

  def this(system: String, user: String, password: String, memory: Int) {
    this(new File(system), user, password, memory)
  }

  def this(system: String, user: String, password: String, memory: Int, vcore: Int) {
    this(new File(system), user, password, memory, vcore)
  }

  def launchAVirtualMachine: IVirtualMachine = {

    class VirtualMachineConnector extends IConnectable {
      var virtualMachine: IVirtualMachine = null

      override def connect(port: Int) = {
        val qemuDir = getQEmuDir
        val commandLine = CommandLine.parse(new File(qemuDir, "qemu").getAbsolutePath() + " -m " + memory + " -smp " + vcore + " -nographic -hda " + systemResource.getDeployedFile().getAbsolutePath() + " -L " + qemuDir.getAbsolutePath() + " -redir tcp:" + port + "::22")

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
 
    val timeOut = Activator.getWorkspace().getPreferenceAsDurationInMs(Configuration.VirtualMachineBootTimeOut).intValue
    val connection = new Connection(virtualMachine.host, virtualMachine.port)
    connection.connect(new ServerHostKeyVerifier() {
        override def verifyServerHostKey(hostname: String, port: Int, serverHostKeyAlgorithm: String, serverHostKey: Array[Byte]): Boolean = {
          true
        }
      }, timeOut, timeOut)
      
    connection.close


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
          FileUtil.copy(this.getClass().getClassLoader().getResource(qemuJarPath + f).openStream(), outputStream)
        } finally {
          outputStream.close
        }
        dest.setExecutable(true)
      } )

    qemuDir
  }
}
