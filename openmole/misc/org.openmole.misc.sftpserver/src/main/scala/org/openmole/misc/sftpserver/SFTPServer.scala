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

package org.openmole.misc.sftpserver

import java.io.File
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
import org.apache.sshd.common.Session
import org.apache.sshd.server.sftp.SftpSubsystem
import org.openmole.misc.tools.service.ThreadUtil._
import collection.JavaConversions._

class SFTPServer(path: File, login: String, password: String, port: Int) {
  val sshd = SshServer.setUpDefaultServer

  {
    sshd.setPort(port)
    sshd.setSubsystemFactories(List(new SftpSubsystem.Factory))
    sshd.setCommandFactory(new ScpCommandFactory)
    sshd.setFileSystemFactory(new FileSystemFactory {
      override def createFileSystemView(s: Session) = new NativeFileSystemView(login, false) {
        override def getFile(file: String) = {
          val sandboxed =
            if (file.startsWith(path.getCanonicalPath)) new File(file)
            else new File(path, file)

          if (sandboxed.getCanonicalPath.startsWith(path.getCanonicalPath)) super.getFile(sandboxed.getAbsolutePath)
          else super.getFile(path.getAbsolutePath)
        }
      }
    })

    sshd.setPasswordAuthenticator(new PasswordAuthenticator {
      override def authenticate(username: String, pass: String, session: ServerSession) = {
        username == login && pass == password
      }
    })
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider)

    start
  }

  override def finalize = background { stop }

  def start = sshd.start

  def stop = sshd.stop

}
