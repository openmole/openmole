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
import java.util.concurrent.Semaphore
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.eclipse.jetty.server.{ Server, ServerConnector }
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp._
import org.openmole.core.tools.io.Network
import org.openmole.core.workspace.{ ConfigurationLocation, Workspace }
import org.scalatra.auth.strategy.{ BasicAuthStrategy, BasicAuthSupport }
import org.scalatra.servlet.ScalatraListener
import javax.servlet.ServletContext

import org.scalatra._
import org.eclipse.jetty.util.resource.{ Resource ⇒ Res }
import org.openmole.gui.server.jscompile.JSPack
import org.openmole.tool.hash._
import org.openmole.tool.file._

object GUIServer {
  def isPasswordCorrect(p: String) = Workspace.passwordIsCorrect(p)
  def resourcePath = (Workspace.openMOLELocation / "webapp").getAbsolutePath

  val portValue = Network.freePort
  val port = ConfigurationLocation("GUIServer", "Port", Some(portValue))
  Workspace setPreferenceIfNotSet (port, portValue)

  lazy val lockFile = {
    val file = Workspace.file("GUI.lock")
    file.createNewFile
    file
  }

  lazy val urlFile = Workspace.file("GUI.url")

  val servletArguments = "servletArguments"
  case class ServletArguments(passwordCorrect: String ⇒ Boolean, applicationControl: ApplicationControl)
  case class ApplicationControl(restart: () ⇒ Unit, stop: () ⇒ Unit)

  sealed trait ExitStatus
  case object Restart extends ExitStatus
  case object Ok extends ExitStatus

}

class GUIBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    val args = context.get(GUIServer.servletArguments).get.asInstanceOf[GUIServer.ServletArguments]
    context mount (new GUIServlet(args), "/*")
  }
}

import GUIServer._

class GUIServer(port: Int, localhost: Boolean, http: Boolean) {

  val server = new Server()
  var exitStatus: GUIServer.ExitStatus = GUIServer.Ok
  val semaphore = new Semaphore(0)

  lazy val contextFactory = {
    val contextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory()
    val ks = Workspace.keyStore
    contextFactory.setKeyStore(ks)
    contextFactory.setKeyStorePassword(Workspace.keyStorePassword)
    contextFactory.setKeyManagerPassword(Workspace.keyStorePassword)
    contextFactory.setTrustStore(ks)
    contextFactory.setTrustStorePassword(Workspace.keyStorePassword)
    contextFactory
  }

  val connector = if (!http) new ServerConnector(server, contextFactory) else new ServerConnector(server)
  connector.setPort(port)
  if (!localhost) connector.setHost("localhost")

  server.addConnector(connector)

  val context = new WebAppContext()
  val applicationControl =
    ApplicationControl(
      () ⇒ { exitStatus = GUIServer.Restart; stop() },
      () ⇒ stop()
    )
  context.setAttribute(GUIServer.servletArguments, GUIServer.ServletArguments(GUIServer.isPasswordCorrect, applicationControl))
  context.setContextPath("/")
  context.setResourceBase(resourcePath)
  context.setClassLoader(classOf[GUIServer].getClassLoader)
  context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[GUIBootstrap].getCanonicalName)
  context.addEventListener(new ScalatraListener)

  server.setHandler(context)

  def start() = server.start

  def join(): GUIServer.ExitStatus = {
    semaphore.acquire()
    semaphore.release()
    exitStatus
  }

  def stop() = {
    semaphore.release()
    server.stop()
  }

}
