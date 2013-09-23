package org.openmole.web

import _root_.akka.actor.ActorSystem
import org.scalatra._
import scalate.ScalateSupport
import servlet.{ FileItem, FileUploadSupport }
import java.io.{ PrintStream, File, InputStream }
import javax.servlet.annotation.MultipartConfig

import org.openmole.core.serializer._
import org.openmole.core.model.mole.{ IPartialMoleExecution, IMoleExecution, ExecutionContext }
import org.openmole.core.model.data.{ Context, Prototype, Variable }
import com.thoughtworks.xstream.mapper.CannotResolveClassException
import concurrent.Future

import slick.driver.H2Driver.simple._
import slick.jdbc.meta.MTable

import org.openmole.misc.tools.io.FromString

import org.openmole.misc.eventdispatcher.EventDispatcher

import Database.threadLocalSession

import javax.sql.rowset.serial.SerialClob
import reflect.ClassTag
import json.JacksonJsonSupport
import org.json4s._
import org.json4s.JsonDSL._
import scala.None
import org.json4s.{ Formats, DefaultFormats }
import org.openmole.core.implementation.validation.Validation
import org.openmole.core.implementation.validation.DataflowProblem.{ MissingSourceInput, MissingInput, WrongType }
import scala.io.Source
import org.openmole.core.model.mole.IMoleExecution.JobStatusChanged
import org.openmole.misc.eventdispatcher.{ Event, EventListener }

@MultipartConfig(maxFileSize = 3 * 1024 * 1024 /*max file size of 3 MiB*/ ) //research scala multipart config
class MoleRunner(val system: ActorSystem) extends ScalatraServlet with SlickSupport with ScalateSupport
    with FileUploadSupport with FlashMapSupport with FutureSupport with JacksonJsonSupport with MoleHandling {

  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

  /*val cachedMoles = new DataHandler[String, IMoleExecution](system)
  val moleStats = new DataHandler[String, Stats.Stats](system)

  val listener: EventListener[IMoleExecution] = new JobEventListener(moleStats)*/

  get("/index.html") {
    contentType = "text/html"

    new AsyncResult() {
      val is = Future {
        ssp("/index.ssp")
      }
    }
  }

  get("/") {
    new AsyncResult() {
      val is = Future { redirect(url("index.html")) }
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

  post("/createMole") {

    val data = fileParams.get("file")

    val inS = data.map(_.getInputStream)

    val csv = fileParams.get("csv")

    val cnS = csv.map(_.getInputStream)

    val encapsulate = params get "encapsulate" match {
      case Some("on") ⇒ true
      case _          ⇒ false
    }

    //TODO: make sure this is released

    new AsyncResult {
      val is = Future {

        contentType = "text/html"

        createMole(inS, cnS, encapsulate) match {
          case Left(error) ⇒ ssp("/createMole", "body" -> "Please upload a serialized mole execution below!", "errors" -> List(error))
          case _           ⇒ redirect(url("execs"))
        }
      }
    }
  }

  /*//todo: update this for async
  post("/xml/createMole") {
    val moleExec = processXMLFile[IMoleExecution](fileParams.get("file"), fileParams.get("file").map(_.getInputStream))

    moleExec match {
      case (Some(exec), _) ⇒ {
        cachedMoles.add(exec.id, exec)
        redirect("/xml/execs")
      }
      case (_, error) ⇒ <error>{ error }</error>
    }
  }*/

  post("/json/createMole") {
    contentType = formats("json")

    val encapsulate = params get "encapsulate" match {
      case Some("on") ⇒ true
      case _          ⇒ false
    }

    val res = createMole(fileParams get "file" map (_.getInputStream), fileParams get "csv" map (_.getInputStream), encapsulate)

    res match {
      case Left(error) ⇒ Xml.toJson(<error>{ error }</error>)
      case Right(exec) ⇒ Xml.toJson(<moleID>{ exec.id }</moleID>)

    }
  }

  get("/execs") {
    new AsyncResult() {
      contentType = "text/html"

      val is = Future {
        ssp("/loadedExecutions", "ids" -> getMoleKeys)
      }
    }
  }

  get("/execs/:id") {
    new AsyncResult() {
      val is = Future {
        contentType = "text/html"

        val pRams = params("id")

        val pageData = getMoleStats(pRams) + ("Encapsulated" -> isEncapsulated(pRams)) + ("id" -> pRams) + ("status" -> getStatus(pRams))

        ssp("/executionData", pageData.toSeq: _*)
      }
    }
  }

  get("/json/execs/:id") {
    contentType = formats("json")

    val pRams = params("id")

    val stats = getMoleStats(pRams)
    val r = getStatus(pRams)

    render(("status", r) ~
      ("stats", stats.toSeq))
  }

  get("/json/execs") {
    contentType = formats("json")

    render(("execIds", getMoleKeys))
  }

  get("/start/:id") {
    contentType = "text/html"

    new AsyncResult() {
      val is = Future {

        startMole(params("id"))

        redirect("/execs/" + params("id"))
      }
    }
  }

  get("/data/:id/data.tar") {
    contentType = "application/octet-stream"
    getMoleResult(params("id"))
  }

  /*get("/xml/execs") {
    contentType = "text/xml"

    <mole-execs>
      { for (key ← cachedMoles.getKeys) yield <execID>{ key }</execID> }
    </mole-execs>
  }

  get("/xml/start/:id") {
    contentType = "text/html"

    new AsyncResult() {
      val is = Future {
        val exec = cachedMoles.get(params("id"))
        println(exec)

        val res = exec map { x ⇒ x.start; "started" }

        <exec-result> { res.getOrElse("id didn't exist") } </exec-result>
      }
    }
  }*/

  get("/json/start/:id") {
    contentType = formats("json")

    val exec = getMole(params("id"))

    render(("id", exec map (_.id) getOrElse "none") ~
      ("execResult", exec map { e ⇒ e.start; getStatus(params("id")) } getOrElse "id didn't exist"))
  }

  get("/json/remove/:id") {
    contentType = formats("json")

    val exec = deleteMole(params("id"))

    render(("id", exec map (_.id) getOrElse "none") ~
      ("status", "deleted"))
  }

  get("/remove/:id") {
    new AsyncResult() {
      val is = Future {
        deleteMole(params("id"))

        redirect("/execs")
      }
    }
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    resourceNotFound()
  }
}
