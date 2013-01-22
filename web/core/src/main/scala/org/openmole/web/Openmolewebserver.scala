package org.openmole.web

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext

class Openmolewebserver(port: Int) {

  val server = new Server(port)

  val context = new WebAppContext()

  val res = classOf[Openmolewebserver].getResource("/WEB-INF/")

  context setContextPath "/"
  context.setResourceBase(res.toString)

  server.setHandler(context)

  def start() {
    server.start
    server.join
  }

  def end() {
    server.stop
    server.join
  }
}