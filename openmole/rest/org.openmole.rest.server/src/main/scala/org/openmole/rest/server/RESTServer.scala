package org.openmole.rest.server

import javax.servlet.ServletContext

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.util.resource.{ Resource ⇒ Res }
import org.eclipse.jetty.webapp.WebAppContext
import org.openmole.core.workspace._
import org.openmole.tool.logger.JavaLogger
import org.scalatra.ScalatraBase
import org.scalatra.servlet.ScalatraListener
import org.eclipse.jetty.security.{ ConstraintMapping, ConstraintSecurityHandler }
import org.openmole.core.preference.Preference
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.threadprovider.ThreadProvider
import org.scalatra._
import org.openmole.tool.hash._
import org.openmole.tool.crypto._
import org.openmole.tool.file._
import org.openmole.tool.random.Seeder
import org.openmole.core.services._

object RESTServer extends JavaLogger {
  def isPasswordCorrect(cypher: Cypher)(implicit preference: Preference) =
    Preference.passwordIsCorrect(cypher, preference)
}

import RESTServer.Log._

object RESTLifeCycle {
  def arguments = "arguments"
  case class Arguments(services: Services)
}

class RESTLifeCycle extends LifeCycle {

  override def init(context: ServletContext) {
    val args = context.getAttribute(RESTLifeCycle.arguments).asInstanceOf[RESTLifeCycle.Arguments]
    context.mount(
      new RESTAPI {
        lazy val arguments = args
      },
      "/*"
    )
  }

}

class RESTServer(port: Option[Int], hostName: Option[String], services: Services, subDir: Option[String]) {

  private lazy val server = {
    val portValue = port getOrElse 8080

    val server = new Server()
    logger.info(s"binding HTTP REST API to port $portValue")

    val connector = new org.eclipse.jetty.server.ServerConnector(server)
    connector.setPort(portValue)
    server.addConnector(connector)

    val context = new WebAppContext()

    context.setContextPath(subDir.map { s ⇒ "/" + s }.getOrElse("") + "/")
    context.setBaseResource(Res.newResource(classOf[RESTServer].getClassLoader.getResource("/")))
    context.setClassLoader(classOf[RESTServer].getClassLoader)
    hostName foreach (context.setInitParameter(ScalatraBase.HostNameKey, _))
    context.setInitParameter("org.scalatra.Port", portValue.toString)
    context.setInitParameter(ScalatraBase.ForceHttpsKey, true.toString)

    context.setAttribute(RESTLifeCycle.arguments, RESTLifeCycle.Arguments(services))
    context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[RESTLifeCycle].getCanonicalName)
    context.addEventListener(new ScalatraListener)

    //    val constraintHandler = new ConstraintSecurityHandler
    //    val constraintMapping = new ConstraintMapping
    //    constraintMapping.setPathSpec("/*")
    //    constraintMapping.setConstraint({
    //      val r = new org.eclipse.jetty.util.security.Constraint(); r.setDataConstraint(1); r
    //    })
    //    constraintHandler.addConstraintMapping(constraintMapping)
    //
    //    context.setSecurityHandler(constraintHandler)
    server.setHandler(context)
    server
  }

  def run() {
    server.start
    server.join
  }

  def end() {
    server.stop
    server.join
  }
}