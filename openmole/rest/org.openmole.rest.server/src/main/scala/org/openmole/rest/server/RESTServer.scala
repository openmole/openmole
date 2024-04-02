package org.openmole.rest.server

import java.util.concurrent.Semaphore
import scala.concurrent.duration.Duration

import org.openmole.core.workspace._
import org.openmole.tool.logger.JavaLogger
import org.openmole.core.preference.Preference
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.threadprovider.ThreadProvider
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
import org.openmole.gui.server.ext.utils.HTTP

object RESTServer extends JavaLogger:
  def isPasswordCorrect(cypher: Cypher)(implicit preference: Preference) =
    Preference.passwordIsCorrect(cypher, preference)

class RESTServer(port: Option[Int], localhost: Boolean/*, hostName: Option[String]*/, services: Services):

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
      withResponseHeaderTimeout(Duration.Inf).
      withServiceErrorHandler(r => t => HTTP.stackError(t))


  def run() =
    implicit val runtime = cats.effect.unsafe.IORuntime.global
    RESTServer.Log.logger.info(s"binding HTTP REST API to port $portValue")
    val (_, stop) = server.allocated.unsafeRunSync() // feRunSync()._2
    stopSemaphore.acquire()
    stop.unsafeRunSync()

  def stop = stopSemaphore.release()




