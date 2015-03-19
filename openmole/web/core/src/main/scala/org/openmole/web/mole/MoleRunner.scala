package org.openmole.web.mole

import _root_.akka.actor.ActorSystem
import org.openmole.core.eventdispatcher.Event
import org.openmole.core.tools.io.FromString
import org.openmole.core.workflow.validation.DataflowProblem
import org.scalatra._
import servlet.{ FileItem, FileUploadSupport }
import java.io.{ PrintStream, InputStream }
import javax.servlet.annotation.MultipartConfig
import concurrent.Future

import slick.driver.H2Driver.simple._

import json.JacksonJsonSupport
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.{ Formats, DefaultFormats }
import DataflowProblem.{ MissingSourceInput, MissingInput }
import org.openmole.web.Authentication
import org.openmole.web.db.SlickDB

@MultipartConfig(maxFileSize = 3145728 /*max file size of 3 MiB*/ ) //research scala multipart config
class MoleRunner(val system: ActorSystem, val database: SlickDB /*TODO: is this safe??*/ ) extends ScalatraServlet
    with FileUploadSupport with FlashMapSupport with FutureSupport with JacksonJsonSupport with MoleHandling with Authentication {

  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

  private val logger = org.openmole.web.Log.log

  post("/getApiKey") {
    contentType = "text/plain"
    logger.info("received apiKey request")
    try {
      request.headers get "pass" map issueKey foreach (cookies("apiKey") = _)
      cookies get "apiKey" foreach (k ⇒ logger.info(s"created api key: $k"))
      ""
    }
    catch {
      case e: InvalidPasswordException ⇒ "Invalid password entered"
    }
  }

  post("/json/getApiKey") {
    contentType = formats("json")

    try {
      request.headers get "pass" map issueKey map (render("apiKey", _)) getOrElse render("error", "no password sent with request")
    }
    catch {
      case e: InvalidPasswordException ⇒ render(("error", e.getMessage) ~ ("stackTrace", e.getStackTrace.map(e ⇒ s"\tat$e").reduceLeft((prev, next) ⇒ s"$prev\n$next")))
    }
  }

  post("/xml/getApiKey") {
    contentType = "application/xml"

    try {
      request.headers get "pass" map issueKey map (k ⇒ <apiKey>{ k }</apiKey>) getOrElse <error>"no password sent with request"</error>
    }
    catch {
      case e: InvalidPasswordException ⇒ <error><message>{ e.getMessage }</message><stackTrace>{ e.getStackTrace.map(e ⇒ s"\tat $e").reduceLeft((prev, next) ⇒ s"$prev\n$next") }</stackTrace></error>
    }

  }

  post("/xml/createMole") {
    contentType = "application/xml"

    logger.info(request.headers get "apiKey" toString)

    requireAuth(request.headers get "apiKey") {
      logger.info("starting the create operation")
      val encapsulate = params get "encapsulate" match {
        case Some("on") ⇒ true
        case _          ⇒ false
      }

      val molePack = params get "pack" match {
        case Some("on") ⇒ true
        case _          ⇒ false
      }

      logger.info("encapsulate and pack parsed")

      val res = createMole(fileParams get "file" map (_.getInputStream), fileParams get "csv" map (_.getInputStream), encapsulate, molePack)

      logger.info("mole created")
      logger.info(res.toString)

      res match {
        case Left(error) ⇒ <error>{ error }</error>
        case Right(exec) ⇒ <moleID>{ exec.id }</moleID>
      }
    } {
      <error>"This service requires a password"</error>
    }
  }

  post("/json/createMole") {
    contentType = formats("json")

    requireAuth(request.headers get "apiKey") {
      val encapsulate = params get "encapsulate" match {
        case Some("on") ⇒ true
        case _          ⇒ false
      }

      val molePack = params get "pack" match {
        case Some("on") ⇒ true
        case _          ⇒ false
      }

      val res = createMole(fileParams get "file" map (_.getInputStream), fileParams get "csv" map (_.getInputStream), encapsulate, pack = molePack)

      res match {
        case Left(error) ⇒ Xml.toJson(<error>{ error }</error>)
        case Right(exec) ⇒ Xml.toJson(<moleID>{ exec.id }</moleID>)
      }
    } {
      Xml.toJson(<error>"This service requires a password"</error>)
    }
  }

  get("/json/execs/:id") {
    contentType = formats("json")

    val pRams = params("id")

    val r = getStatus(pRams)

    render(("status", r) ~ ("stats", getWebStats(pRams)))
  }

  get("/json/execs") {
    contentType = formats("json")

    render(("execIds", getMoleKeys))
  }

  get("/data/:id/data.tar") {
    contentType = "application/octet-stream"
    getMoleResult(params("id"))
  }

  get("/xml/execs") {
    contentType = "application/xml"

    <mole-execs>
      { for (key ← getMoleKeys) yield <moleID>{ key }</moleID> }
    </mole-execs>
  }

  get("/xml/execs/:id") {
    contentType = "application/xml"

    val pRams = params("id")

    val stats = getWebStats(pRams).toMap
    val r = getStatus(pRams)

    <status current={ r }>
      {
        for (stat ← stats.keys) <stat id={ stat }>{ stats(stat) }</stat>
      }
    </status>

  }

  get("/xml/start/:id") {
    contentType = "text/html"

    val exec = getMole(params("id"))
    val res = exec map { x ⇒ x.start; "started" }

    <exec-result> { res.getOrElse("id didn't exist") } </exec-result>
  }

  get("/json/start/:id") {
    contentType = formats("json")

    val exec = getMole(params("id"))

    render(("id", exec map (_.id) getOrElse "none") ~
      ("execResult", exec map { e ⇒ e.start; getStatus(params("id")) } getOrElse "id didn't exist"))
  }

  get("/xml/start/:id") {
    contentType = "application/xml"

    val exec = getMole(params("id"))

    <moleID status={ exec map { e ⇒ e.start; getStatus(params("id")) } getOrElse ("id doesn't exist") }>{ params("id") }</moleID>
  }

  get("/json/remove/:id") {
    contentType = formats("json")

    requireAuth(request.headers get "apiKey") {
      val exec = deleteMole(params("id"))

      render(("id", exec map (_.id) getOrElse "none") ~
        ("status", "deleted"))
    } {
      render(("error", "This service requires a password"))
    }
  }

  get("/xml/remove/:id") {
    contentType = "application/xml"

    new AsyncResult() {
      val is = Future {
        requireAuth(request.headers get "apiKey") {
          val exec = deleteMole(params("id"))

          <moleID status={ if (exec.isDefined) "deleted" else "id doesn't exist" }>{ params("id") }</moleID>
        } {
          <error>"This service requires a password"</error>
        }
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
