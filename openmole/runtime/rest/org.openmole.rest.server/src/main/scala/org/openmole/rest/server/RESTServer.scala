package org.openmole.rest.server

import javax.servlet.ServletContext

import org.bouncycastle.operator.{ DefaultDigestAlgorithmIdentifierFinder, DefaultSignatureAlgorithmIdentifierFinder }
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.util.resource.{ Resource ⇒ Res }
import org.eclipse.jetty.webapp.WebAppContext
import org.openmole.core.console.ScalaREPL
import org.openmole.tool.service.Logger
import org.openmole.core.workflow.task.PluginSet
import org.openmole.core.workspace.{ ConfigurationLocation, Workspace }
import org.openmole.tool.crypto.Certificate
import org.scalatra.ScalatraBase
import java.security.{ Security, SecureRandom, KeyPairGenerator, KeyStore }
import java.io.{ FileOutputStream, FileInputStream }
import org.scalatra.servlet.ScalatraListener

import org.eclipse.jetty.security.{ ConstraintMapping, ConstraintSecurityHandler }
import org.scalatra._
import org.openmole.console._
import org.openmole.tool.hash._

object RESTServer extends Logger {

  def configure = {
    Console.initPassword
    setPassword(Console.askPassword("Server password"))
  }

  def passwordHash = ConfigurationLocation("REST", "PasswordHash", true)
  def setPassword(p: String) = Workspace.setPreference(passwordHash, p.hash.toString)
  def isPasswordCorrect(p: String) = Workspace.preference(passwordHash) == p.hash.toString

}

import RESTServer.Log._

object RESTLifeCycle {
  def arguments = "arguments"
  case class Arguments(plugins: PluginSet)
}

class RESTLifeCycle extends LifeCycle {

  override def init(context: ServletContext) {
    val args = context.getAttribute(RESTLifeCycle.arguments).asInstanceOf[RESTLifeCycle.Arguments]
    context.mount(
      new RESTAPI {
        def arguments = args
      },
      "/*"
    )
  }

}

class RESTServer(port: Option[Int], sslPort: Option[Int], hostName: Option[String], allowInsecureConnections: Boolean, plugins: PluginSet) {

  private lazy val server = {
    val p = port getOrElse 8080
    val sslP = sslPort getOrElse 8443

    val server =
      if (allowInsecureConnections) {
        logger.info(s"Binding http to port $p")
        new Server(p)
      }
      else new Server()

    val contextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory()
    val ks = Workspace.keyStore
    contextFactory.setKeyStore(ks)
    contextFactory.setKeyStorePassword(Workspace.keyStorePassword)
    contextFactory.setKeyManagerPassword(Workspace.keyStorePassword)
    contextFactory.setTrustStore(ks)
    contextFactory.setTrustStorePassword(Workspace.keyStorePassword)

    logger.info(s"binding https to port $sslP")

    server.addConnector(
      new org.eclipse.jetty.server.ssl.SslSelectChannelConnector(contextFactory) {
        setPort(sslP)
        //setMaxIdleTime(30000)
      }
    )

    val context = new WebAppContext()

    context.setContextPath("/")
    context.setBaseResource(Res.newResource(classOf[RESTServer].getClassLoader.getResource("/")))
    context.setClassLoader(classOf[RESTServer].getClassLoader)
    hostName foreach (context.setInitParameter(ScalatraBase.HostNameKey, _))
    context.setInitParameter("org.scalatra.Port", sslP.toString)
    context.setInitParameter(ScalatraBase.ForceHttpsKey, allowInsecureConnections.toString)

    context.setAttribute(RESTLifeCycle.arguments, RESTLifeCycle.Arguments(plugins))
    context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[RESTLifeCycle].getCanonicalName)
    context.addEventListener(new ScalatraListener)

    val constraintHandler = new ConstraintSecurityHandler
    val constraintMapping = new ConstraintMapping
    constraintMapping.setPathSpec("/*")
    constraintMapping.setConstraint({
      val r = new org.eclipse.jetty.util.security.Constraint(); r.setDataConstraint(1); r
    })
    constraintHandler.addConstraintMapping(constraintMapping)

    if (!allowInsecureConnections) context.setSecurityHandler(constraintHandler)
    server.setHandler(context)
    server
  }

  def start() {
    server.start
    server.join
  }

  def end() {
    server.stop
    server.join
  }
}