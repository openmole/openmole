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

package org.openmole.plugin.task.external

import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.job.IContext
import ch.ethz.ssh2._
import org.openmole.plugin.tools.utils.SSHUtils._
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Callable
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.plugin.resource.virtual.IVirtualMachine
import org.openmole.plugin.task.external.internal.Activator._
import scala.collection.JavaConversions._
import org.openmole.core.implementation.tools.VariableExpansion._

abstract class ExternalVirtualTask(name: String, relativeDir: String) extends ExternalTask(name) {

  def this(name: String) {
    this(name, "")
  }
  
  implicit def callable[T](f: () => T): Callable[T] =  new Callable[T]() { def call() = f() }

  def prepareInputFiles(global: IContext, context: IContext, progress: IProgress, vmDir: String, client: SFTPv3Client) {
    listInputFiles(global, context, progress).foreach( f => {
        val to = vmDir + '/' + f.name
        copyTo(client, f.file, to)
      })
  }


  def fetchOutputFiles(global: IContext, context: IContext, progress: IProgress, vmDir: String, destDir: File, client: SFTPv3Client) = {
    setOutputFilesVariables(global, context,progress,destDir).foreach( f => {
        val from = vmDir + '/' + f.name
        copyFrom(client, from, f.file)
      })
  }


  protected def execute(global: IContext, context: IContext, progress: IProgress, cmd: String, vm: IVirtualMachine, user: String, password: String) = {
    val connection = getSSHConnection(vm, user, password, 0) //workspace.getPreferenceAsDurationInMs(Configuration.VirtualMachineConnectionTimeOut).intValue )
    try {
      val sftp = new SFTPv3Client(connection)

      try {
        val tmpDir = "/tmp/" + UUID.randomUUID + '/'
        sftp.mkdir(tmpDir, 0x777)

        prepareInputFiles(global, context, progress, tmpDir, sftp)
        
        val workDir = if(!relativeDir.isEmpty) tmpDir + relativeDir + '/' else tmpDir
        val session = connection.openSession

        try {
          session.execCommand("cd " + workDir + " ; " + expandData(global, context, CommonVariables(workDir), cmd))
          waitForCommandToEnd(session, 0)
        } finally {
          session.close
        }

        fetchOutputFiles(global, context, progress, workDir, workspace.newDir, sftp)
        delete(sftp,tmpDir)
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
    var connected = false
    while(!connected) {
      try {
        connection.connect(null, timeOut, timeOut)
        connected = true
      } catch {
        case e: IOException => Logger.getLogger(classOf[ExternalVirtualTask].getName).log(Level.WARNING, "Error durring SSH connexion, retrying...", e)
      }
    }
    
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
