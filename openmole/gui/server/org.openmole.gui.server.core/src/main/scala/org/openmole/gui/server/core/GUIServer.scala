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

import java.util.concurrent.Semaphore
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.eclipse.jetty.server.{ Server, ServerConnector }
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp._
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.scalatra.servlet.ScalatraListener
import javax.servlet.ServletContext

import org.scalatra._
import org.eclipse.jetty.util.resource.{ Resource ⇒ Res }
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.gui.server.jscompile.JSPack
import org.openmole.tool.hash._
import org.openmole.tool.file._
import org.openmole.core.services._
import org.openmole.tool.crypto.KeyStore
import org.openmole.tool.network.Network
import org.openmole.core.location._

object GUIServer {

  def webapp()(implicit newFile: NewFile, workspace: Workspace, fileService: FileService) = {
    val from = openMOLELocation / "webapp"
    val to = newFile.newDir("webapp")
    from.copy(to)
    Utils.expandDepsFile(from / "js" / Utils.depsFileName, from / "js" / Utils.openmoleGrammarName, to /> "js" / Utils.depsFileName)
    Utils.openmoleFile copy (to /> "js" / Utils.openmoleFileName)
    to
  }

  val port = ConfigurationLocation("GUIServer", "Port", Some(Network.freePort))

  def initialisePreference(preference: Preference) = {
    if (!preference.isSet(port)) preference.setPreference(port, Network.freePort)
  }

  def lockFile(implicit workspace: Workspace) = {
    val file = Utils.webUIDirectory() / "GUI.lock"
    file.createNewFile
    file
  }

  def urlFile(implicit workspace: Workspace) = Utils.webUIDirectory() / "GUI.url"

  val servletArguments = "servletArguments"
  case class ServletArguments(
    services:           GUIServices,
    password:           Option[String],
    applicationControl: ApplicationControl,
    webapp:             File,
    extraHeader:        String
  )

  case class ApplicationControl(restart: () ⇒ Unit, stop: () ⇒ Unit)

  sealed trait ExitStatus

  case object Restart extends ExitStatus

  case object Ok extends ExitStatus

}

class GUIBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    val args = context.get(GUIServer.servletArguments).get.asInstanceOf[GUIServer.ServletArguments]
    context mount (GUIServlet(args), "/*")
  }
}

import GUIServer._

class GUIServer(port: Int, localhost: Boolean, http: Boolean, services: GUIServices, password: Option[String], extraHeader: String) {

  val server = new Server()
  var exitStatus: GUIServer.ExitStatus = GUIServer.Ok
  val semaphore = new Semaphore(0)

  import services._

  lazy val contextFactory = {
    val contextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory()
    def keyStorePassword = "openmole"
    val ks = KeyStore(services.workspace.persistentDir /> "keystoregui", keyStorePassword)
    contextFactory.setKeyStore(ks.keyStore)
    contextFactory.setKeyStorePassword(keyStorePassword)
    contextFactory.setKeyManagerPassword(keyStorePassword)
    contextFactory.setTrustStore(ks.keyStore)
    contextFactory.setTrustStorePassword(keyStorePassword)
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

  val webappCache = webapp

  context.setAttribute(GUIServer.servletArguments, GUIServer.ServletArguments(services, password, applicationControl, webappCache, extraHeader))
  context.setContextPath("/")

  import services._

  context.setResourceBase(webappCache.getAbsolutePath)
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
