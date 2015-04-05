package org.openmole.rest.server

import javax.servlet.annotation.MultipartConfig
import javax.servlet.http.HttpServletRequest

import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.servlet.FileUploadSupport
import org.openmole.rest.messages

import scala.util.{ Try, Failure, Success }

@MultipartConfig //research scala multipart config
trait RESTAPI extends ScalatraServlet
    with FileUploadSupport
    with FlashMapSupport
    with JacksonJsonSupport
    with Authentication {

  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal
  private val logger = Log.log

  def exceptionToHttpError(e: Throwable) = InternalServerError(render(("error", e.getMessage) ~ ("stackTrace", e.getStackTrace.map(e ⇒ s"\tat$e").reduceLeft((prev, next) ⇒ s"$prev\n$next"))))

  post("/token") {
    contentType = formats("json")

    Try(params("password")) map issueToken match {
      case Failure(_)                                      ⇒ ExpectationFailed(render("error", "No password sent with request"))
      case Success(Failure(InvalidPasswordException(msg))) ⇒ Forbidden(msg)
      case Success(Failure(e))                             ⇒ exceptionToHttpError(e)
      case Success(Success(Token(token, start, end))) ⇒
        val json = pretty(Extraction.decompose(messages.Token(token, end - start)))
        println(json)
        Accepted(json)
    }
  }

  post("/start") {
    contentType = formats("json")

    authenticated {
      (fileParams get "script") match {
        case None ⇒ ExpectationFailed("Missing mandatory script file.")
        case Some(script) ⇒
          logger.info("starting the create operation")

          println(script)

          Ok(script)

        /* val directory = Workspace.newFile

          try {
            val inputDirectory = new File(directory, "inputs")
            val partialMole = deserializePartialMole(mole, packed, inputDirectory)
            val context =
              (fileParams get "context", params get "format") match {
                case (Some(file), _) => Some(csvToContext(file)(partialMole))
                case _ => None
              }

            registerMole(partialMole, context)

        val res =
          createMole(
            serializedMole,
            csvContext,
            encapsulate,
            packed)

        logger.info("mole created")
        logger.info(res.toString)

        res match {
          case Left(error) ⇒ InternalServerError(Xml.toJson(<error>{ error }</error>))
          case Right(exec) ⇒ Ok(Xml.toJson(<moleID>{ exec.id }</moleID>))
        }
          } catch {
            case e: Throwable =>
              directory.recursiveDelete
              exceptionToHttpError(e)
          }*/
      }
    }
  }

  /* get("/execs/:id") {
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

  /*get("/start/:id") {
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
  }*/

  get("/remove/:id") {
    authenticated {
      contentType = formats("json")
      val exec = deleteMole(params("id"))
      Ok(render(("id", exec map (_.id) getOrElse "none") ~ ("status", "deleted")))
    }
  }*/

  def authenticated[T](success: ⇒ ActionResult)(implicit r: HttpServletRequest): ActionResult = {
    def fail = Unauthorized(render(("error", "This service requires a token")))

    r.headers.get("token") match {
      case None    ⇒ fail
      case Some(k) ⇒ if (checkKey(k)) success else fail
    }
  }

}
