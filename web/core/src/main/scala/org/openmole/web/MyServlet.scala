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
import concurrent.{ Await, ExecutionContext, Future }
import org.openmole.web.{ DataHandler, Datastore }
import concurrent.duration._

@MultipartConfig(maxFileSize = 3 * 1024 * 1024) //research scala multipart config
class MyServlet(val system: ActorSystem) extends ScalatraServlet with ScalateSupport with FileUploadSupport with FlashMapSupport with FutureSupport {

  protected implicit def executor: ExecutionContext = system.dispatcher

  get("/index.html") {
    contentType = "text/html"

    new AsyncResult() {
      val is = Future {
        ssp("/index.ssp")
      }
    }
  }

  get("/createMole") {
    contentType = "text/html"
    new AsyncResult() {
      val is = Future {
        ssp("/createMole", "body" -> "Please upload a serialized mole execution below!")
      }
    }
  }

  var moleExecs = Map.empty[String, MoleExecution]
  val testMoleData = new DataHandler[String, MoleExecution](system)

  def getStatus(exec: MoleExecution): String = {
    if (!exec.started)
      "Stopped"
    else if (!exec.finished)
      "Running"
    else
      "Finished"
  }

  def processXMLFile[A](file: Option[FileItem], is: Option[InputStream]) = {
    file match {
      case Some(data) ⇒
        if (data.getContentType.isDefined && data.getContentType.get == "text/xml")
          try {
            is.map(SerializerService.deserialize[A](_)) -> ""
          } catch {
            case e: CannotResolveClassException ⇒ None -> "The uploaded xml was not a valid serialized object."
          } finally {
            is.foreach(_.close())
          }
        else
          None -> "The uploaded data was not of type text/xml"
      case None ⇒ None -> "No data was uploaded."
    }
  }

  post("/createMole") {
    contentType = "text/html"

    implicit def timeout = Duration(10, SECONDS)

    val data = fileParams.get("file")

    //TODO: Make sure this is not a problem
    val ins = fileParams.get("file").map(_.getInputStream)

    val x = new AsyncResult() {
      val is = Future {
        val moleExec = processXMLFile[MoleExecution](data, ins)

        moleExec match {
          case (Some(exec), _) ⇒ {
            //moleExecs += (exec.id -> exec) // can't be used by async pages.
            testMoleData.add(exec.id, exec)
            println("added " + exec.id + "to testMoleData.")
            halt(status = 301, headers = Map("Location" -> url("execs")))
          }
          case (_, error) ⇒ ssp("/createMole", "body" -> "Please upload a serialized mole execution below!", "errors" -> List(error))
        }
      }
    }

    println(x.timeout)
    x
  }

  post("/xml/createMole") {
    val moleExec = processXMLFile[MoleExecution](fileParams.get("file"), fileParams.get("file").map(_.getInputStream))

    moleExec match {
      case (Some(exec), _) ⇒ {
        moleExecs = moleExecs + (exec.id -> exec)
        redirect("/xml/execs")
      }
      case (_, error) ⇒ <error>{ error }</error>
    }
  }

  get("/execs") {
    contentType = "text/html"

    new AsyncResult() {
      val is = Future {
        ssp("/loadedExecutions", "ids" -> testMoleData.getKeys.toList)
      }
    }
  }

  get("/execs/:id") {
    contentType = "text/html"

    val pRams = params("id")

    new AsyncResult() {
      val is = Future {
        testMoleData.get(pRams) match {
          case Some(exec) ⇒ {
            println(getStatus(exec))
            println(exec)
            ssp("/executionData", "id" -> pRams, "status" -> getStatus(exec))
          }
          case _ ⇒ ssp("createMole", "body" -> "no such id")
        }
      }
    }
  }

  get("/start/:id") {

    println("here")

    val x = params("id")
    println("started starting")

    new AsyncResult() {
      override implicit def timeout = Duration(1, MINUTES)
      val is = Future {

        val exec = testMoleData.get(x)
        println(exec)

        exec foreach { x ⇒ x.start; println("started") }
        //halt(status = 301, headers = Map("Location" -> ("/execs/" + x)))
      }
    }
  }

  get("/badAsyncTest") {
    contentType = "text/html"

    new AsyncResult() {
      val is = Future {
        redirect("/createMole")
      }
    }
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
    resourceNotFound()
  }
}
