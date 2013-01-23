package org.openmole.web

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.resource._
import org.eclipse.jetty.webapp.WebAppContext

class Openmolewebserver(port: Int) {

  val server = new Server(port)

  val context = new WebAppContext()

  val res = classOf[Openmolewebserver].getResource("/") //Dear god this is a horrible hack
  val bes = classOf[org.fusesource.scalate.servlet.ServletRenderContext]
  val des = classOf[org.fusesource.scalate.console.ConsoleSnippets]
  val ges = classOf[org.fusesource.scalate.Binding]
  classOf[org.fusesource.scalate.support.Code]

  val ass = classOf[org.eclipse.jetty.servlet.listener.ELContextCleaner]

  println(ass)

  val resor = Resource.newResource(res)

  println(res.toString)

  context.setContextPath("/")
  context.setBaseResource(resor)
  context.setClassLoader(classOf[Openmolewebserver].getClassLoader)

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