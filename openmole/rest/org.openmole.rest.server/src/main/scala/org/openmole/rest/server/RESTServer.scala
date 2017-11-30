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

class RESTServer(sslPort: Option[Int], hostName: Option[String], services: Services) {

  private lazy val server = {
    val sslP = sslPort getOrElse 8443

    val server = new Server()

    val contextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory()
    def keyStorePassword = "openmole"
    val ks = KeyStore(services.workspace.persistentDir / "keystorerest", keyStorePassword)
    contextFactory.setKeyStore(ks.keyStore)
    contextFactory.setKeyStorePassword(keyStorePassword)
    contextFactory.setKeyManagerPassword(keyStorePassword)
    contextFactory.setTrustStore(ks.keyStore)
    contextFactory.setTrustStorePassword(keyStorePassword)

    logger.info(s"binding https to port $sslP")

    val connector = new org.eclipse.jetty.server.ServerConnector(server, contextFactory)
    connector.setPort(sslP)
    server.addConnector(connector)

    val context = new WebAppContext()

    context.setContextPath("/")
    context.setBaseResource(Res.newResource(classOf[RESTServer].getClassLoader.getResource("/")))
    context.setClassLoader(classOf[RESTServer].getClassLoader)
    hostName foreach (context.setInitParameter(ScalatraBase.HostNameKey, _))
    context.setInitParameter("org.scalatra.Port", sslP.toString)
    context.setInitParameter(ScalatraBase.ForceHttpsKey, true.toString)

    context.setAttribute(RESTLifeCycle.arguments, RESTLifeCycle.Arguments(services))
    context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[RESTLifeCycle].getCanonicalName)
    context.addEventListener(new ScalatraListener)

    val constraintHandler = new ConstraintSecurityHandler
    val constraintMapping = new ConstraintMapping
    constraintMapping.setPathSpec("/*")
    constraintMapping.setConstraint({
      val r = new org.eclipse.jetty.util.security.Constraint(); r.setDataConstraint(1); r
    })
    constraintHandler.addConstraintMapping(constraintMapping)

    context.setSecurityHandler(constraintHandler)
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