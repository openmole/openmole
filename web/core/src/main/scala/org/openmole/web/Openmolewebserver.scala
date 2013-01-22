package org.openmole.web

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.resource._
import org.eclipse.jetty.webapp.WebAppContext

class Openmolewebserver(port: Int) {

  val server = new Server(port)

  val context = new WebAppContext()

  val res = classOf[Openmolewebserver].getResource("/")

  val resor = Resource.newResource(res);

  println(res.toString)

  context setContextPath "/"
  context.setBaseResource(resor)

  server.setHandler(context)

  def start() {
    try {
      server.start
      server.join

    } catch {
      case e: Exception ⇒ e.printStackTrace()
    }

  }

  def end() {
    server.stop
    server.join
  }
}