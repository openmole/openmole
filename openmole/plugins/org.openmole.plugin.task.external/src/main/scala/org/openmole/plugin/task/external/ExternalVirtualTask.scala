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
import com.jcraft.jsch.Session
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.SftpException
import org.openmole.plugin.task.external.internal.SSHUtils._
import com.jcraft.jsch._
import java.io.File
import java.io.PrintStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.plugin.task.external.internal.Activator._
import scala.collection.JavaConversions._
import org.openmole.core.implementation.tools.VariableExpansion._

abstract class ExternalVirtualTask(name: String) extends ExternalTask(name) {

  object Configuration {
    val VirtualMachineConnectionTimeOut = new ConfigurationLocation(classOf[ExternalVirtualTask].getSimpleName(), "VirtualMachineConnectionTimeOut")
    workspace.addToConfigurations(VirtualMachineConnectionTimeOut, "PT60S")
    val ActiveWaitInterval = new ConfigurationLocation(classOf[ExternalVirtualTask].getSimpleName(), "ActiveWaitInterval")
    workspace.addToConfigurations(ActiveWaitInterval, "PT1S")
  }

  def prepareInputFiles(context: IContext, progress: IProgress, vmDir: String, channel: ChannelSftp) {
    listInputFiles(context, progress).foreach( f => {
        val to = vmDir + '/' + f.name
        copyTo(channel, f.file, to)
      })
  }


  def fetchOutputFiles(context: IContext, progress: IProgress, vmDir: String, destDir: File, channel: ChannelSftp) = {
    setOutputFilesVariables(context,progress,destDir).foreach( f => {
        val from = vmDir + '/' + f.name
        copyFrom(channel, from, f.file)
      })
  }


  protected def execute(context: IContext, progress: IProgress, cmd: String, session: Session) = {
    session.connect( workspace.getPreferenceAsDurationInMs(Configuration.VirtualMachineConnectionTimeOut).intValue )
    try {

      val channelSftp = session.openChannel("sftp") match {
        case ch: ChannelSftp => ch
        case _ => throw new ClassCastException
      }

      channelSftp.connect
      try {

        val workDir = channelSftp.pwd + '/' + UUID.randomUUID + '/'
        channelSftp.mkdir(workDir)

        prepareInputFiles(context, progress, workDir,channelSftp)
        val channel = session.openChannel("exec") match {
          case ch: ChannelExec => ch
          case _ => throw new ClassCastException
        }

        channel.setCommand("cd " + workDir + " ; " + expandData(context, cmd))

        channel.setOutputStream(new PrintStream(System.out)  {
            override def close() = {}
          })

        channel.setErrStream(new PrintStream(System.err)  {
            override def close() = {}
          })

        // start job
        channel.connect

        try {
          //Ugly active wait
          while(!channel.isClosed) {
            Thread.sleep( workspace.getPreferenceAsDurationInMs(Configuration.ActiveWaitInterval).intValue )
          }
        } finally {
          channel.disconnect
        }

        fetchOutputFiles(context, progress, workDir, workspace.newTmpDir, channelSftp)
        delete(channelSftp,workDir)
      } finally {
        channelSftp.disconnect
      }
    } catch {
      case e: SftpException => throw new InternalProcessingError(e)
      case e: IOException => throw new InternalProcessingError(e)
    } finally {
      session.disconnect
    }
  }



}
