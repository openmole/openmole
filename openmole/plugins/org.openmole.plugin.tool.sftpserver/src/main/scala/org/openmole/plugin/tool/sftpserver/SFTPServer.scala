/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.tool.sftpserver

import java.io.File

import org.apache.sshd.common.file._
import org.apache.sshd.common.file.root.RootedFileSystemProvider
import org.apache.sshd.common.session.Session
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.command.ScpCommandFactory
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory
import org.openmole.tool.hash._
import org.openmole.tool.logger.Logger
import org.openmole.tool.thread._

import scala.collection.JavaConversions._

object SFTPServer extends Logger

class SFTPServer(path: File, port: Int, passwordHash: Hash) {

  @volatile var started = false

  lazy val sshd = {
    val sshd = SshServer.setUpDefaultServer

    def fileSystem = new RootedFileSystemProvider().newFileSystem(path.toPath, Map.empty[String, Object])

    sshd.setPort(port)
    sshd.setSubsystemFactories(List(new SftpSubsystemFactory))
    sshd.setCommandFactory(new ScpCommandFactory)
    sshd.setFileSystemFactory(new FileSystemFactory {
      override def createFileSystem(s: Session) = fileSystem
    })

    sshd.setPasswordAuthenticator(new PasswordAuthenticator {
      override def authenticate(username: String, pass: String, session: ServerSession) =
        pass.hash == passwordHash
    })
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider)
    sshd
  }

  override def finalize = background { stop }

  def startIfNeeded = sshd.synchronized { if (!started) start; started = true }
  def start = sshd.synchronized { sshd.start }
  def stop = sshd.synchronized { if (started) sshd.stop(true); started = false }

}
