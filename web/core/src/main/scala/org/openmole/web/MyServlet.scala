package org.openmole.web

import _root_.akka.actor.{ Props, ActorSystem }
import org.scalatra._
import scalate.ScalateSupport
import servlet.{ FileItem, FileUploadSupport }
import java.io.{ InputStream, File }
import javax.servlet.annotation.MultipartConfig
import xml.XML
import org.openmole.core.serializer._
import org.openmole.core.implementation.mole.MoleExecution
import com.thoughtworks.xstream.mapper.CannotResolveClassException
import concurrent.{ ExecutionContext, Future }
import org.openmole.web.{ DataHandler, Datastore }

@MultipartConfig(maxFileSize = 3 * 1024 * 1024) //research scala multipart config
class MyServlet(val system: ActorSystem) extends ScalatraServlet with ScalateSupport with FileUploadSupport with FlashMapSupport with FutureSupport {

  protected implicit def executor: ExecutionContext = system.dispatcher

  get("/createMole") {
    contentType = "text/html"
    new AsyncResult() {
      def is = Future {
        ssp("/createMole", "body" -> "Please upload a serialized mole execution below!")
      }
    }
  }

  var moleExecs = Map.empty[String, MoleExecution]
  val testMoleData = new DataHandler[String, MoleExecution](system)

  def getStatus(id: String): String = {
    val exec = moleExecs(id)
    if (!exec.started)
      "Stopped"
    else if (!exec.finished)
      "Running"
    else
      "Finished"
  }

  def processXMLFile[A](file: Option[FileItem]) = {
    file match {
      case Some(data) ⇒
        if (data.getContentType.isDefined && data.getContentType.get == "text/xml")
          try {
            Some(SerializerService.deserialize[A](data.getInputStream)) -> ""
          } catch {
            case e: CannotResolveClassException ⇒ None -> "The uploaded xml was not a valid serialized object."
          }
        else
          None -> "The uploaded data was not of type text/xml"
      case None ⇒ None -> "No data was uploaded."
    }
  }

  post("/createMole") {
    contentType = "text/html"
    val moleExec = processXMLFile[MoleExecution](fileParams.get("file"))

    moleExec match {
      case (Some(exec), _) ⇒ {
        moleExecs += (exec.id -> exec)
        testMoleData.add(exec.id, exec)
        redirect("/execs")
      }
      case (_, error) ⇒ ssp("/createMole", "body" -> "Please upload a serialized mole execution below!", "errors" -> List(error))
    }
  }

  post("/xml/createMole") {
    val moleExec = processXMLFile[MoleExecution](fileParams.get("file"))

    moleExec match {
      case (Some(exec), _) ⇒ {
        moleExecs = moleExecs + (exec.id -> exec)
        redirect("/xml/execs")
      }
      case (_, error) ⇒ <error>{ error }</error>
    }
  }

  get("/execs/:id/start") {
    contentType = "text/html"

    moleExecs(params("id")).start

    redirect("/execs/" + params("id"))
  }

  get("/execs") {
    contentType = "text/html"

    ssp("/loadedExecutions", "ids" -> moleExecs.keys.toList)

  }

  get("/execs/:id") {
    contentType = "text/html"

    println(testMoleData.get(params("id")).started)

    ssp("/executionData", "id" -> params("id"), "status" -> getStatus(params("id")))
  }

  get("/xml/execs") {
    contentType = "text/xml"

    <mole-execs>
      { for (key ← moleExecs.keys) yield <execID>{ key }</execID> }
    </mole-execs>
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path ⇒
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }
}
