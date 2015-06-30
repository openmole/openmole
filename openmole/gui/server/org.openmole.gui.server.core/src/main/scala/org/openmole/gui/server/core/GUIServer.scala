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
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp._
import org.openmole.console.Console
import org.openmole.core.tools.io.Network
import org.openmole.core.workspace.{ ConfigurationLocation, Workspace }
import org.scalatra.auth.strategy.{ BasicAuthStrategy, BasicAuthSupport }
import org.scalatra.servlet.ScalatraListener
import javax.servlet.ServletContext
import org.scalatra._
import org.eclipse.jetty.util.resource.{ Resource ⇒ Res }
import org.openmole.tool.hash._

object GUIServer {
  val passwordHash = new ConfigurationLocation("GUIServer", "PasswordHash", true)
  def setPassword(p: String) = Workspace.setPreference(passwordHash, p.hash.toString)
  def isPasswordCorrect(p: String) = Workspace.preference(passwordHash) == p.hash.toString

  def initPassword = {
    Console.initPassword
    if (!Workspace.isPreferenceSet(passwordHash)) setPassword(Console.askPassword("Authentication password"))
  }

  val port = new ConfigurationLocation("GUIServer", "Port")
  Workspace += (port, Network.freePort.toString)

  lazy val lockFile = {
    val file = Workspace.file("GUI.lock")
    file.createNewFile
    file
  }
  lazy val urlFile = Workspace.file("GUI.url")

  val servletArguments = "servletArguments"
  case class ServletArguments(passwordCorrect: Option[String ⇒ Boolean] = None)
}

class GUIServer(port: Int, webapp: File, authentication: Boolean) {

  val server = new Server()

  val contextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory()
  val ks = Workspace.keyStore
  contextFactory.setKeyStore(ks)
  contextFactory.setKeyStorePassword(Workspace.keyStorePassword)
  contextFactory.setKeyManagerPassword(Workspace.keyStorePassword)
  contextFactory.setTrustStore(ks)
  contextFactory.setTrustStorePassword(Workspace.keyStorePassword)

  val connector = new org.eclipse.jetty.server.ssl.SslSelectChannelConnector(contextFactory)
  connector.setPort(port)
  if (!authentication) connector.setHost("localhost")

  server.addConnector(connector)

  val context = new WebAppContext()
  val authenticationMethod = if (authentication) Some(GUIServer.isPasswordCorrect _) else None
  context.setAttribute(GUIServer.servletArguments, GUIServer.ServletArguments(authenticationMethod))
  context.setContextPath("/")
  context.setResourceBase(webapp.getAbsolutePath)
  context.setClassLoader(classOf[GUIServer].getClassLoader)
  context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[ScalatraBootstrap].getCanonicalName)
  context.addEventListener(new ScalatraListener)

  server.setHandler(context)

  def start() = server.start
  def join() = server.join
  def end() = server.stop

}
