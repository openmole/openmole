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

package org.openmole.plugin.environment.desktop

import org.openmole.core.batch.environment.Authentication
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.JobService
import org.openmole.core.batch.environment.VolatileStorage
import java.util.concurrent.Executors
import org.apache.sshd.SshServer
import org.apache.sshd.common.Session
import org.apache.sshd.server.FileSystemFactory
import org.apache.sshd.server.FileSystemView
import org.apache.sshd.server.PasswordAuthenticator
import org.apache.sshd.server.SshFile
import org.apache.sshd.server.auth.UserAuthPassword
import org.apache.sshd.server.command.ScpCommandFactory
import org.apache.sshd.server.filesystem.NativeFileSystemFactory
import org.apache.sshd.server.filesystem.NativeFileSystemView
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.sftp.SftpSubsystem
import org.openmole.core.batch.control.JobServiceDescription
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.io.FileUtil._
import java.io.File
import collection.JavaConversions._
import org.openmole.misc.tools.service.ThreadUtil._

object DesktopEnvironment {
  val timeStempsDirName = "timeStemps"
  val jobsDirName = "jobs"
  val resultsDirName = "results"
}

class DesktopEnvironment(port: Int, login: String, password: String, inRequieredMemory: Option[Int]) extends BatchEnvironment(inRequieredMemory) {
  
  def this(port: Int, login: String, password: String) = this(port, login, password, None)
  
  val path = Workspace.newDir
  val sshd = SshServer.setUpDefaultServer
  
  {
    sshd.setPort(port)
    sshd.setSubsystemFactories(List(new SftpSubsystem.Factory))
    sshd.setCommandFactory(new ScpCommandFactory)
    sshd.setFileSystemFactory(new FileSystemFactory {
        override def createFileSystemView(s: String) = new NativeFileSystemView(login, false) {        
          //println("fs view " + s)
          override def getFile(file: String) = {
            val sandboxed = new File(path, file)
          //println("getFlie " + file)
            
            //println("Desktop " + sandboxed.toString)
            if(sandboxed.getCanonicalPath.startsWith(path.getCanonicalPath)) super.getFile(sandboxed.getAbsolutePath)
            else super.getFile(path.getAbsolutePath)
          }
        }
      })
    
    sshd.setPasswordAuthenticator(new PasswordAuthenticator {
        override def authenticate(username: String, pass: String, session: ServerSession) = {
          username == login && pass == password
        }})
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider)

    sshd.start
  }
  
  override def finalize = background{sshd.stop}
  
  @transient lazy val batchStorage = new VolatileStorage(this, path.toURI, Int.MaxValue)
  
  @transient override lazy val allStorages = List(batchStorage)
  @transient override lazy val allJobServices = List(new DesktopJobService(this, new JobServiceDescription(path.getAbsolutePath)))
  
  override def authentication = DesktopAuthentication
}
