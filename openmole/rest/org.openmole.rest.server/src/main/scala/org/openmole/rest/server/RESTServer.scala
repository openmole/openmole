package org.openmole.rest.server

import java.util.concurrent.Semaphore
import javax.servlet.ServletContext
import scala.concurrent.duration.Duration

//import org.eclipse.jetty.server.Server
//import org.eclipse.jetty.servlet.DefaultServlet
//import org.eclipse.jetty.util.resource.{ Resource ⇒ Res }
//import org.eclipse.jetty.webapp.WebAppContext
import org.openmole.core.workspace._
import org.openmole.tool.logger.JavaLogger
import org.scalatra.ScalatraBase
//import org.scalatra.servlet.ScalatraListener
//import org.eclipse.jetty.security.{ ConstraintMapping, ConstraintSecurityHandler }
import org.openmole.core.preference.Preference
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.threadprovider.ThreadProvider
//import org.scalatra._
import org.openmole.tool.hash._
import org.openmole.tool.crypto._
import org.openmole.tool.file._
import org.openmole.tool.random.Seeder
import org.openmole.core.services._

import cats.effect.*
import org.http4s.*
import org.http4s.blaze.server.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.headers.{Location, `Content-Type`}
import org.http4s.syntax.all.*
import org.http4s.server.*

object RESTServer extends JavaLogger {
  def isPasswordCorrect(cypher: Cypher)(implicit preference: Preference) =
    Preference.passwordIsCorrect(cypher, preference)
}

//import RESTServer.Log._
//
//object RESTLifeCycle {
//  def arguments = "arguments"
//  case class Arguments(services: Services)
//}
//
//class RESTLifeCycle extends LifeCycle {
//
//  override def init(context: ServletContext) = {
//    val args = context.getAttribute(RESTLifeCycle.arguments).asInstanceOf[RESTLifeCycle.Arguments]
//    context.mount(
//      new RESTAPI {
//        val arguments = args
//      },
//      "/*"
//    )
//  }
//
//}

class RESTServer(port: Option[Int], localhost: Boolean/*, hostName: Option[String]*/, services: Services, subDir: Option[String]):

  def portValue = port getOrElse 8080

  lazy val stopSemaphore = new Semaphore(0)

  private lazy val server =
    def server(port: Int, localhost: Boolean) =
      val s =
        if (localhost) BlazeServerBuilder[IO].bindHttp(port, "localhost")
        else BlazeServerBuilder[IO].bindHttp(port, "0.0.0.0")
      s.enableHttp2(true)

    val restAPI = RESTAPI(services)
    
    val httpApp = Router("/" -> restAPI.routes).orNotFound

    server(portValue, localhost).
      withHttpApp(httpApp).
      withIdleTimeout(Duration.Inf).
      withResponseHeaderTimeout(Duration.Inf)//.withServiceErrorHandler(r => t => stackError(t))

//    val server = new Server()
//    logger.info(s"binding HTTP REST API to port $portValue")
//
//    val connector = new org.eclipse.jetty.server.ServerConnector(server)
//    connector.setPort(portValue)
//    server.addConnector(connector)
//
//    val context = new WebAppContext()
//
//    context.setContextPath(subDir.map { s ⇒ "/" + s }.getOrElse("") + "/")
//    context.setBaseResource(Res.newResource(classOf[RESTServer].getClassLoader.getResource("/")))
//    context.setClassLoader(classOf[RESTServer].getClassLoader)
//    hostName foreach (context.setInitParameter(ScalatraBase.HostNameKey, _))
//    context.setInitParameter("org.scalatra.Port", portValue.toString)
//    context.setInitParameter(ScalatraBase.ForceHttpsKey, true.toString)
//
//    context.setAttribute(RESTLifeCycle.arguments, RESTLifeCycle.Arguments(services))
//    context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[RESTLifeCycle].getCanonicalName)
//    context.addEventListener(new ScalatraListener)
//
//    server.setHandler(context)
//    server


  def run() =
    implicit val runtime = cats.effect.unsafe.IORuntime.global
    RESTServer.Log.logger.info(s"binding HTTP REST API to port $portValue")
    val (_, stop) = server.allocated.unsafeRunSync() // feRunSync()._2
    stopSemaphore.acquire()
    stop.unsafeRunSync()
//    server.start
//    server.join

  def stop = stopSemaphore.release()
//  def end() = {
//    server.stop
//    server.join
//  }
