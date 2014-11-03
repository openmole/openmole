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
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import javax.servlet.ServletContext
import org.scalatra._
import org.eclipse.jetty.util.resource.{ Resource ⇒ Res }

class GUIServer(port: Option[Int], webapp: File) {
  val p = port getOrElse 8080

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

}
