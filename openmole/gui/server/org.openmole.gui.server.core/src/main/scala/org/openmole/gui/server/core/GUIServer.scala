package org.openmole.gui.server.core

/*
 * Copyright (C) 22/09/14 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp._
import org.openmole.core.workspace.Workspace
import org.scalatra.servlet.ScalatraListener
import javax.servlet.ServletContext
import org.scalatra._
import org.eclipse.jetty.util.resource.{ Resource ⇒ Res }

object GUIServer {
  lazy val lockFile = {
    val file = Workspace.file("GUI.lock")
    file.createNewFile
    file
  }
  lazy val urlFile = Workspace.file("GUI.url")
}

class GUIServer(port: Int, webapp: File) {

  val server = new Server()

  val contextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory()
  val ks = Workspace.keyStore
  contextFactory.setKeyStore(ks)
  contextFactory.setKeyStorePassword(Workspace.keyStorePassword)
  contextFactory.setKeyManagerPassword(Workspace.keyStorePassword)
  contextFactory.setTrustStore(ks)
  contextFactory.setTrustStorePassword(Workspace.keyStorePassword)

  server.addConnector(
    new org.eclipse.jetty.server.ssl.SslSelectChannelConnector(contextFactory) {
      setPort(port)
    }
  )

  val context = new WebAppContext()
  context.setContextPath("/")
  context.setResourceBase(webapp.getAbsolutePath)
  context.setClassLoader(classOf[GUIServer].getClassLoader)
  context.addEventListener(new ScalatraListener)

  server.setHandler(context)

  def start() = server.start
  def join() = server.join
  def end() = server.stop

}
