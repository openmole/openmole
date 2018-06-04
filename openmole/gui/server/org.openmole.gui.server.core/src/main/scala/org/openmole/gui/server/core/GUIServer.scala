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

  def fromWebAppLocation = openMOLELocation / "webapp"

  def webapp(optimizedJS: Boolean)(implicit newFile: NewFile, workspace: Workspace, fileService: FileService) = {
    val from = fromWebAppLocation
    val to = newFile.newDir("webapp")

    from / "css" copy to / "css"
    from / "fonts" copy to / "fonts"
    from / "img" copy to / "img"

    Utils.expandDepsFile(from / "js" / Utils.depsFileName, from / "js" / Utils.openmoleGrammarName, to /> "js" / Utils.depsFileName)
    Utils.openmoleFile(optimizedJS) copy (to /> "js" / Utils.openmoleFileName)
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
    extraHeader:        String,
    subDir:             Option[String]
  )

  case class ApplicationControl(restart: () ⇒ Unit, stop: () ⇒ Unit)

  sealed trait ExitStatus

  case object Restart extends ExitStatus

  case object Ok extends ExitStatus

}

class GUIBootstrap extends LifeCycle {
  override def init(context: ServletContext) = {
    val args = context.get(GUIServer.servletArguments).get.asInstanceOf[GUIServer.ServletArguments]
    context mount (GUIServlet(args), "/*")
  }
}

class StartingPage extends ScalatraServlet with LifeCycle {

  override def init(context: ServletContext) = {
    context mount (this, "/*")
  }

  get("/") {
    def content =
      <html>
        <meta http-equiv="refresh" content="3;url=/"/>
        <link href="/css/style.css" rel="stylesheet"/>
        <body>
          <div>OpenMOLE is loading...<div class="loader" style="float: right"></div><br/></div>
          (for the first launch, and after an update, it may take several minutes)
        </body>
      </html>

    ServiceUnavailable(content)
  }

}

import GUIServer._

class GUIServer(port: Int, localhost: Boolean, http: Boolean, services: GUIServices, password: Option[String], extraHeader: String, optimizedJS: Boolean, subDir: Option[String]) {

  val server = new Server()
  var exitStatus: GUIServer.ExitStatus = GUIServer.Ok
  val semaphore = new Semaphore(0)

  import services._

  def start() = {
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

    val startingContext = new WebAppContext()
    startingContext.setClassLoader(classOf[StartingPage].getClassLoader)
    startingContext.setInitParameter(ScalatraListener.LifeCycleKey, classOf[StartingPage].getCanonicalName)
    startingContext.setResourceBase(fromWebAppLocation.getAbsolutePath)
    startingContext.setContextPath("/")
    startingContext.setContextPath(subDir.map { s ⇒ "/" + s }.getOrElse("") + "/")
    startingContext.addEventListener(new ScalatraListener)

    server.setHandler(startingContext)
    server.start()
  }

  def launchApplication() = {
    val context = new WebAppContext()
    val applicationControl =
      ApplicationControl(
        () ⇒ {
          exitStatus = GUIServer.Restart
          stop()
        },
        () ⇒ stop()
      )

    val webappCache = webapp(optimizedJS)

    context.setAttribute(GUIServer.servletArguments, GUIServer.ServletArguments(services, password, applicationControl, webappCache, extraHeader, subDir))

    context.setContextPath(subDir.map { s ⇒ "/" + s }.getOrElse("") + "/")

    import services._

    context.setResourceBase(webappCache.getAbsolutePath)
    context.setClassLoader(classOf[GUIServer].getClassLoader)
    context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[GUIBootstrap].getCanonicalName)
    context.addEventListener(new ScalatraListener)

    server.stop()
    server.setHandler(context)
    server.start()
  }

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
