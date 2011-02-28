/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.resource.virtual

import ch.ethz.ssh2.Connection
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.net.Socket
import java.util.concurrent.Callable
import java.util.concurrent.TimeoutException
import java.util.logging.Level
import java.util.logging.Logger
import java.util.concurrent.TimeUnit
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.ShutdownHookProcessDestroyer
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FileUtil
import org.openmole.misc.tools.io.Network.IConnectable
import org.openmole.misc.executorservice.ExecutorService
import org.openmole.misc.tools.io.StringBuilderOutputStream
import org.openmole.core.model.task.IResource
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.io.Network._
import org.openmole.plugin.tools.utils.ProcessUtils._

object VirtualMachineResource {
  val VMBootTime = new ConfigurationLocation("VirtualMachine", "VMBootTime")
  Workspace += (VMBootTime, "PT5M")

  val commonFiles = Array("bios.bin")
  val executable = "qemu"
}

class VirtualMachineResource(val system: File, val user: String, val password: String, memory: Int, vcore: Int) extends IResource {

  import VirtualMachineResource._

  def this(system: String, user: String, password: String, memory: Int, vcore: Int) = this(new File(system), user, password, memory, vcore)
  
  def this(system: File, user: String, password: String, memory: Int) = this(system, user, password, memory, 1)
  
  def this(system: String, user: String, password: String, memory: Int) = this(new File(system), user, password, memory)
  
  def this(system: File, user: String, password: String) = this(system, user, password, 256)
  
  def this(system: String, user: String, password: String) = this(new File(system), user, password)
    
  
  def launchAVirtualMachine: IVirtualMachine = {
    if (!system.isFile) throw new UserBadDataError("Image " + system.getAbsolutePath() + " doesn't exist or is not a file.");
        
    val vmImage = Workspace.newFile
    FileUtil.copy(system, vmImage)
 
    class VirtualMachineConnector extends IConnectable {

      var virtualMachine: IVirtualMachine = null

      override def connect(port: Int) = {
       
        val commandLine = new CommandLine(new File(qEmuDir, executable))
        commandLine.addArguments("-m " + memory + " -smp " + vcore + " -redir tcp:" + port + "::22 -nographic -hda ");
        commandLine.addArgument(system.getAbsolutePath());
        commandLine.addArguments("-L");
        commandLine.addArgument(qEmuDir.getAbsolutePath);
        commandLine.addArguments("-monitor null -serial none");

        val process = Runtime.getRuntime.exec(commandLine.toString)
        //Prevent qemu network from working on Windoze?
        //Process process = commandLauncher().exec(commandLine, new HashMap());
        processDestroyer.add(process)

        virtualMachine = new VirtualMachine("localhost", port, process, processDestroyer, vmImage)
      }
    }

    val connector = new VirtualMachineConnector

       
    connectToFreePort(connector)
    val ret = connector.virtualMachine
      
    val timeOut = Workspace.preferenceAsDurationInMs(VMBootTime)

    val connectionFuture = ExecutorService.executorService(ExecutorType.OWN).submit(new Callable[Unit] {

        override def call = {
              
          var connected = false
          while (!connected) {
            try {
              val socket = new Socket(ret.host, ret.port)
              socket.close
              val connection = new Connection(ret.host, ret.port)
              connection.connect(null, 0, 0)
              connection.close
              //socket.close();
              connected = true
            } catch {
              case ex => Logger.getLogger(classOf[VirtualMachineResource].getName()).log(Level.WARNING, "Problem durring the connection, retrying...", ex);
            }
          }
        }
      })

    try {
      connectionFuture.get(timeOut, TimeUnit.MILLISECONDS)
    } catch {
      case e: TimeoutException => {
          connectionFuture.cancel(true)
          connector.virtualMachine.shutdown
          throw e
        }
      case e => {
          connector.virtualMachine.shutdown
          throw e
        }
    }

    connector.virtualMachine
  }

  @transient private lazy val processDestroyer = new ShutdownHookProcessDestroyer

  @transient private lazy val virtualMachinePool = new VirtualMachinePool(this)

  @transient lazy val virtualMachineShared = new VirtualMachineShared(this)

  @transient private lazy val qEmuDir = {
    val os = System.getProperty("os.name")

    val qemuDir = Workspace.newDir
        
    var qemuJarPath: String = null
    var toCopy: Array[String] = null

    if (os.toLowerCase.contains("linux")) {
      val process = Runtime.getRuntime().exec("uname -a")

      val builder = new StringBuilder
      executeProcess(process, new PrintStream(new StringBuilderOutputStream(builder)), System.err)
      val res = builder.toString

      if (res.contains("x86_64")) {
        qemuJarPath = "/qemu_linux_64/"
      } else {
        qemuJarPath = "/qemu_linux_32/"
      }
      toCopy = Array(executable)
    } else if (os.toLowerCase().contains("windows")) {
      qemuJarPath = "/qemu_windows/";
      toCopy = Array(executable + ".exe", "SDL.dll")
    } else if (os.toLowerCase.contains("mac")) {
      if (System.getProperty("os.version").contains("10.6")) {
        qemuJarPath = "/qemu_OSX-10.6/";
        toCopy = Array(executable)
      } else {
        throw new InternalProcessingError("Unsuported OSX version " + System.getProperty("os.version"));
      }
    } else {
      throw new InternalProcessingError("Unsuported OS " + os);
    }

    for (f <- toCopy) {
      val qemu = new File(qemuDir, f)
      val outputStream = new BufferedOutputStream(new FileOutputStream(qemu))
      try {
        val is = this.getClass.getClassLoader.getResource(qemuJarPath + f).openStream
        try {
          FileUtil.copy(is, outputStream)
          qemu.setExecutable(true)
        } finally is.close
      } finally {
        outputStream.close
      }
    }
    for (f <- commonFiles) {
      val dest = new File(qemuDir, f)
      val outputStream = new BufferedOutputStream(new FileOutputStream(dest))

      try {
        FileUtil.copy(this.getClass.getClassLoader.getResource(f).openStream, outputStream)
      } finally {
        outputStream.close
      }
    }
    qemuDir
  }

   
  override def deploy {}
}
