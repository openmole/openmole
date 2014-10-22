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
import java.net.URL
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.workspace.Workspace
import org.scalatra.servlet.ScalatraListener
import org.osgi.framework.Bundle
import scala.collection.JavaConverters._
import javax.servlet.ServletContext
import org.scalatra._
import org.openmole.misc.tools.io.FileUtil
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.fileservice._

class GUIServer(bundles: List[Bundle], port: Option[Int], optimized: Boolean = true) {
  val p = port getOrElse 8080

  //Copy all the fixed resources in the workspace if required
  val webui = Workspace.file("webui")
  val jsSrc = new File(webui, "js/src")
  val jsCompiled = new File(webui, "js/compiled")
  val webapp = new File(webui, "webapp")
  jsSrc.mkdirs
  jsCompiled.mkdirs
  webapp.mkdirs

  new File(webapp, "js").mkdirs
  new File(webapp, "css").mkdirs
  new File(webapp, "fonts").mkdirs
  new File(webapp, "WEB-INF").mkdirs

  val thisBundle = PluginManager.bundleForClass(classOf[GUIServer])
  copyURL(thisBundle.findEntries("/", "*.js", true).asScala)
  copyURL(thisBundle.findEntries("/", "*.css", true).asScala)
  copyURL(thisBundle.findEntries("/", "web.xml", true).asScala)

  //Generates js files if
  // - the sources changed or
  // - the optimized js does not exists in optimized mode or
  // - the not optimized js does not exists in not optimized mode
  jsSrc.updateIfChanged(JSPack(bundles, _, jsCompiled, optimized))
  if (optimized && !new File(jsCompiled, JSPack.OPTIMIZED).exists ||
    !optimized && !new File(jsCompiled, JSPack.NOT_OPTIMIZED).exists)
    JSPack(bundles, jsSrc, jsCompiled, optimized)

  val server = new Server(p)

  val context = new WebAppContext()

  context.setContextPath("/")
  context.setResourceBase(webapp.getAbsolutePath)
  context.setClassLoader(classOf[GUIServer].getClassLoader)
  context.addEventListener(new ScalatraListener)

  server.setHandler(context)

  def start() = {
    server.start
    server.join
  }

  def end() {
    server.stop
    server.join
  }

  def copyURL(url: Iterator[URL]) = {
    url.foreach { u ⇒
      u.openStream.copy(new File(webui, u.getFile))
    }
  }
}
