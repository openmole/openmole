package org.openmole.web

import org.scalatra._
import scalate.ScalateSupport
import servlet.{ FileUploadSupport }
import java.io.{ InputStream, File }
import javax.servlet.annotation.MultipartConfig
import xml.XML
import org.openmole.core.serializer._
import org.openmole.core.implementation.mole.MoleExecution

@MultipartConfig(maxFileSize = 3 * 1024 * 1024) //research scala multipart config
class MyServlet extends ScalatraServlet with ScalateSupport with FileUploadSupport with FlashMapSupport {
  get("/bort.html") {
    contentType = "text/html"
    ssp("/bort", "body" -> "<img src=\"images/small-bart.jpg\"></img>\n        <h1>Hello, world!</h1>\n        Say<a href=\"hello-scalate\">hello to Scalate</a>.")
  }

  println("servlet init")
  var moleExecs: Map[String, MoleExecution] = Map("test" -> null)

  post("/bort.html") {
    contentType = "text/html"
    val moleExec = fileParams.get("imgfile") match {
      case Some(data) ⇒
        if (data.getContentType.isDefined && data.getContentType.get == "text/xml")
          Some(SerializerService.deserialize[MoleExecution](data.getInputStream))
        else
          None
      case None ⇒ None
    }

    moleExec foreach { exec ⇒
      exec.start
      println(exec.finished)
      moleExecs = moleExecs + (exec.id -> exec)
    }

    redirect("/loadedExecutions")
  }

  get("/loadedExecutions") {
    contentType = "text/html"

    ssp("/loadedExecutions", "moleExecs" -> moleExecs.keys.toList)

  }

  post("/xml/addMole") {
    println(XML.loadString(request.body))
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
