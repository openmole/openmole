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

package org.openmole.plugin.task.external

import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.job.IContext
import ch.ethz.ssh2._
import org.openmole.plugin.task.external.internal.SSHUtils._
import java.io.File
import java.io.PrintStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Callable
import org.openmole.plugin.resource.virtual.IVirtualMachine
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.plugin.task.external.internal.Activator._
import scala.collection.JavaConversions._
import org.openmole.core.implementation.tools.VariableExpansion._
import org.openmole.commons.tools.service.Retry.retry

abstract class ExternalVirtualTask(name: String) extends ExternalTask(name) {

  implicit def callable[T](f: () => T): Callable[T] =  new Callable[T]() { def call() = f() }

//  object Configuration {
//    val VirtualMachineConnectionTimeOut = new ConfigurationLocation(classOf[ExternalVirtualTask].getSimpleName(), "VirtualMachineConnectionTimeOut")
//    workspace.addToConfigurations(VirtualMachineConnectionTimeOut, "PT2M")
//  }

  def prepareInputFiles(context: IContext, progress: IProgress, vmDir: String, client: SFTPv3Client) {
    listInputFiles(context, progress).foreach( f => {
        val to = vmDir + '/' + f.name
        copyTo(client, f.file, to)
      })
  }


  def fetchOutputFiles(context: IContext, progress: IProgress, vmDir: String, destDir: File, client: SFTPv3Client) = {
    setOutputFilesVariables(context,progress,destDir).foreach( f => {
        val from = vmDir + '/' + f.name
        copyFrom(client, from, f.file)
      })
  }


  protected def execute(context: IContext, progress: IProgress, cmd: String, vm: IVirtualMachine, user: String, password: String) = {
    val connection = getSSHConnection(vm, user, password, 0) //workspace.getPreferenceAsDurationInMs(Configuration.VirtualMachineConnectionTimeOut).intValue )
    try {
      val sftp = new SFTPv3Client(connection)

      try {
        val workDir = "/tmp/" + UUID.randomUUID + '/'
        sftp.mkdir(workDir, 0x777)

        prepareInputFiles(context, progress, workDir, sftp)
        
        val session = connection.openSession

        try {
          session.execCommand("cd " + workDir + " ; " + expandData(context, cmd))
          waitForCommandToEnd(session, 0)
        } finally {
          session.close
        }

        fetchOutputFiles(context, progress, workDir, workspace.newTmpDir, sftp)
        delete(sftp,workDir)
      } finally {
        sftp.close
      }
    } finally {
      connection.close
    }
  }

  def getSSHConnection(virtualMachine: IVirtualMachine, user: String, password: String, timeOut: Int): Connection = {
    val connection = new Connection(virtualMachine.host, virtualMachine.port)

    //Not supossed to fail but sometimes it does
 //   retry( () => {
  //      try{
    connection.connect(null, timeOut, timeOut)
   //     } catch {
    //      case e: Exception => throw e
     //   }
     // } ,workspace.getPreferenceAsInt(Configuration.SSHConnectionRetry))
    
    val isAuthenticated = connection.authenticateWithPassword(user, password)

    if (!isAuthenticated)
      throw new IOException("Authentication failed.")

    connection
  }

}
