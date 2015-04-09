package org.openmole.rest.server

import javax.servlet.annotation.MultipartConfig
import javax.servlet.http.HttpServletRequest

import org.json4s.JsonDSL._
import org.json4s._
import org.openmole.console.Console
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.dsl._
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.servlet.FileUploadSupport
import org.openmole.rest.messages

import scala.io.Source
import scala.util.{ Try, Failure, Success }

@MultipartConfig //research scala multipart config
trait RESTAPI extends ScalatraServlet
    with FileUploadSupport
    with FlashMapSupport
    with JacksonJsonSupport
    with Authentication {

  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal
  private val logger = Log.log
  implicit class ToJsonDecorator(x: Any) {
    def toJson = pretty(Extraction.decompose(x))
  }

  def arguments: RESTLifeCycle.Arguments

  def exceptionToHttpError(e: Throwable) = InternalServerError(messages.Error(e).toJson)

  post("/token") {
    contentType = formats("json")

    Try(params("password")) map issueToken match {
      case Failure(_)                                      ⇒ ExpectationFailed(render("error", "No password sent with request"))
      case Success(Failure(InvalidPasswordException(msg))) ⇒ Forbidden(msg)
      case Success(Failure(e))                             ⇒ exceptionToHttpError(e)
      case Success(Success(Token(token, start, end))) ⇒
        Accepted(messages.Token(token, end - start).toJson)
    }
  }

  post("/start") {
    contentType = formats("json")

    authenticated {
      (params get "script") match {
        case None ⇒ ExpectationFailed("Missing mandatory script parameter.")
        case Some(script) ⇒
          logger.info("starting the create operation")

          val console = new Console(arguments.plugins)
          val repl = console.newREPL()

          Try(repl.eval(script)) match {
            case Failure(e) ⇒ InternalServerError(messages.Error(e).toJson)
            case Success(o) ⇒
              o match {
                case puzzle: Puzzle ⇒
                  val ex = puzzle.start
                  ex.waitUntilEnded
                  Ok("running")
                case _ ⇒ InternalServerError(messages.Error("The last line of the script should be a puzzle", None))
              }
          }

        /*val repl = arguments.repl()

          Try(repl.eval(script)) match {
            case Success(_) ⇒
            case Failure(e) ⇒ println(repl.errorMessages.head.error)
          }
*/

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

    Try(params("token")(r)) match {
      case Failure(_) ⇒ fail
      case Success(k) ⇒ if (checkToken(k)) success else fail
    }
  }

}
