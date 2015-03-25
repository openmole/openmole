package org.openmole.web.mole

import javax.servlet.http.HttpServletRequest

import _root_.akka.actor.ActorSystem
import org.openmole.core.eventdispatcher.Event
import org.openmole.core.tools.io.FromString
import org.openmole.core.workflow.validation.DataflowProblem
import org.scalatra._
import servlet.{ FileItem, FileUploadSupport }
import java.io.{ PrintStream, InputStream }
import javax.servlet.annotation.MultipartConfig
import concurrent.Future
import scala.util.{ Failure, Success }

import slick.driver.H2Driver.simple._

import json.JacksonJsonSupport
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.{ Formats, DefaultFormats }
import DataflowProblem.{ MissingSourceInput, MissingInput }
import org.openmole.web.Authentication
import org.openmole.web.db.SlickDB

@MultipartConfig //research scala multipart config
class MoleRunner(val system: ActorSystem, val database: SlickDB /*TODO: is this safe??*/ ) extends ScalatraServlet
    with FileUploadSupport with FlashMapSupport with JacksonJsonSupport with MoleHandling with Authentication {

  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

  private val logger = org.openmole.web.Log.log

  def noPasswordSent = "No password sent with request"
  def passwordRequired = "This service requires a password"

  post("/token") {
    contentType = formats("json")

    request.headers get "pass" map issueKey match {
      case None               ⇒ ExpectationFailed(render("error", noPasswordSent))
      case Some(Failure(e))   ⇒ InternalServerError(render(("error", e.getMessage) ~ ("stackTrace", e.getStackTrace.map(e ⇒ s"\tat$e").reduceLeft((prev, next) ⇒ s"$prev\n$next"))))
      case Some(Success(key)) ⇒ Ok(render("token", key))
    }
  }

  post("/createMole") {
    authenticated {
      contentType = formats("json")

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
        case Left(error) ⇒ InternalServerError(Xml.toJson(<error>{ error }</error>))
        case Right(exec) ⇒ Ok(Xml.toJson(<moleID>{ exec.id }</moleID>))
      }
    }
  }

  get("/execs/:id") {
    authenticated {
      contentType = formats("json")
      val id = params("id")
      getStatus(id) match {
        case None    ⇒ NotFound(s"Execution with id $id has not been found.")
        case Some(r) ⇒ Ok(render(("status", r) ~ ("stats", getWebStats(id))))
      }
    }
  }

  get("/execs") {
    authenticated {
      contentType = formats("json")
      Ok(render(("execIds", getMoleKeys)))
    }
  }

  get("/data/:id/data.tar") {
    authenticated {
      contentType = "application/octet-stream"
      val id = params("id")
      getMoleResult(id) match {
        case None    ⇒ NotFound(s"Result for id $id has not found.")
        case Some(f) ⇒ Ok(f)
      }
    }
  }

  get("/start/:id") {
    authenticated {
      contentType = formats("json")

      val id = params("id")
      val exec = getMole(id)

      exec match {
        case None ⇒ NotFound(s"No mole registered for $id.")
        case Some(e) ⇒
          e.start
          Ok()
      }
    }
  }

  get("/remove/:id") {
    authenticated {
      contentType = formats("json")
      val exec = deleteMole(params("id"))
      Ok(render(("id", exec map (_.id) getOrElse "none") ~ ("status", "deleted")))
    }
  }

  def authenticated[T](success: ⇒ ActionResult)(implicit r: HttpServletRequest): ActionResult = {
    def fail = Unauthorized(render(("error", "This service requires a token")))

    r.headers.get("token") match {
      case None    ⇒ fail
      case Some(k) ⇒ if (checkKey(k)) success else fail
    }
  }

}
