package org.openmole.web

import org.scalatra._
import scalate.ScalateSupport
import servlet.{ FileItem, FileUploadSupport }
import java.io.{ InputStream, File }
import javax.servlet.annotation.MultipartConfig
import xml.XML
import org.openmole.core.serializer._
import org.openmole.core.implementation.mole.MoleExecution
import com.thoughtworks.xstream.mapper.CannotResolveClassException

@MultipartConfig(maxFileSize = 3 * 1024 * 1024) //research scala multipart config
class MyServlet extends ScalatraServlet with ScalateSupport with FileUploadSupport with FlashMapSupport {
  get("/bort.html") {
    contentType = "text/html"
    ssp("/bort", "body" -> "<img src=\"images/small-bart.jpg\"></img>\n        <h1>Hello, world!</h1>\n        Say<a href=\"hello-scalate\">hello to Scalate</a>.")
  }

  println("servlet init")
  var moleExecs: Map[String, MoleExecution] = Map("test" -> null)

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

  post("/bort.html") {
    contentType = "text/html"
    val moleExec = processXMLFile[MoleExecution](fileParams.get("imgfile"))

    moleExec match {
      case (Some(exec), _) ⇒ {
        moleExecs = moleExecs + (exec.id -> exec)
        redirect("/execs")
      }
      case (_, error) ⇒ ssp("/bort.html", "body" -> "", "errors" -> List(error))
    }
  }

  post("/xml/bort") {
    val moleExec = processXMLFile[MoleExecution](fileParams.get("imgfile"))

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
