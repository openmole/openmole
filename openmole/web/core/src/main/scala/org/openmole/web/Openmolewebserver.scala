package org.openmole.web

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.resource._
import org.eclipse.jetty.webapp.WebAppContext

class Openmolewebserver(port: Int) {

  val server = new Server(port)

  val context = new WebAppContext()

  val res = Resource.newResource(classOf[Openmolewebserver].getResource("/"))

  context.setContextPath("/")
  context.setBaseResource(res)
  context.setClassLoader(classOf[Openmolewebserver].getClassLoader)

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