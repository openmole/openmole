package org.openmole.rest.server

import java.io.{ File, PrintStream }
import java.util.UUID
import java.util.zip.{ GZIPOutputStream, GZIPInputStream }
import javax.servlet.annotation.MultipartConfig
import javax.servlet.http.HttpServletRequest
import groovy.ui.ConsoleView
import org.json4s.JsonDSL._
import org.json4s._
import org.openmole.console._
import org.openmole.core.eventdispatcher._
import org.openmole.core.workflow.mole.{ MoleExecution, ExecutionContext }
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workspace.{ Persistent, Workspace }
import org.openmole.tool.tar.{ TarOutputStream, TarInputStream }
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.servlet.FileUploadSupport
import org.openmole.rest.message._
import org.openmole.tool.file._
import org.openmole.tool.tar._
import scala.util.{ Try, Failure, Success }

case class Execution(workDirectory: WorkDirectory, moleExecution: MoleExecution)

case class WorkDirectory(baseDirectory: File) {
  lazy val inputDirectory = {
    val f = new File(baseDirectory, "inputs")
    f.mkdirs()
    f
  }

  lazy val outputDirectory = {
    val f = new File(baseDirectory, "outputs")
    f.mkdirs()
    f
  }

  def output = new File(baseDirectory, "output")
  lazy val outputStream = new PrintStream(output.bufferedOutputStream())

  def readOutput = {
    outputStream.flush
    output.content
  }

  def clean = {
    outputStream.close
    baseDirectory.recursiveDelete
  }

}

@MultipartConfig(fileSizeThreshold = 1024 * 1024) //research scala multipart config
trait RESTAPI extends ScalatraServlet with GZipSupport
    with FileUploadSupport
    with FlashMapSupport
    with JacksonJsonSupport
    with Authentication {

  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal
  private val logger = Log.log

  private lazy val moles = DataHandler[ExecutionId, Execution]()

  implicit class ToJsonDecorator(x: Any) {
    def toJson = pretty(Extraction.decompose(x))
  }

  def arguments: RESTLifeCycle.Arguments
  def baseDirectory = Workspace.newDir("rest")

  def exceptionToHttpError(e: Throwable) = InternalServerError(Error(e).toJson)

  post("/token") {
    Try(params("password")) map issueToken match {
      case Failure(_)                                      ⇒ ExpectationFailed(Error("No password sent with request").toJson)
      case Success(Failure(InvalidPasswordException(msg))) ⇒ Forbidden(Error(msg).toJson)
      case Success(Failure(e))                             ⇒ exceptionToHttpError(e)
      case Success(Success(AuthenticationToken(token, start, end))) ⇒ Accepted(Token(token, end - start).toJson)
    }
  }

  post("/start") {
    authenticated {
      (params get "script") match {
        case None ⇒ ExpectationFailed(Error("Missing mandatory script parameter.").toJson)
        case Some(script) ⇒
          logger.info("starting the create operation")

          val id = ExecutionId(UUID.randomUUID().toString)
          val directory = WorkDirectory(baseDirectory / id.id)

          def extract =
            for {
              archive ← fileParams get "inputDirectory"
            } {
              val is = new TarInputStream(new GZIPInputStream(archive.getInputStream))
              try is.extract(directory.inputDirectory) finally is.close
            }

          def launch: ActionResult = {
            val console = new Console(arguments.plugins)
            val repl = console.newREPL(ConsoleVariables(inputDirectory = directory.inputDirectory, outputDirectory = directory.outputDirectory))
            Try(repl.eval(script)) match {
              case Failure(e) ⇒
                directory.clean
                ExpectationFailed(Error(e).toJson)
              case Success(o) ⇒
                o match {
                  case puzzle: Puzzle ⇒
                    Try(puzzle.toExecution(executionContext = ExecutionContext(out = directory.outputStream)).start) match {
                      case Success(ex) ⇒
                        moles.add(id, Execution(directory, ex))
                        Ok(id.toJson)
                      case Failure(error) ⇒
                        directory.clean
                        ExpectationFailed(Error(error).toJson)
                    }
                  case _ ⇒
                    directory.clean
                    ExpectationFailed(Error("The last line of the script should be a puzzle"))
                }
            }
          }

          extract
          launch
      }
    }
  }

  post("/outputDirectory") {
    authenticated {
      getExecution { ex ⇒
        val gzOs = response.getOutputStream.toGZ
        val os = new TarOutputStream(gzOs)
        contentType = "application/octet-stream"
        response.setHeader("Content-Disposition", "attachment; filename=" + "outputDirectory.tgz")
        os.archive(ex.workDirectory.outputDirectory)
        os.close()
        Ok()
      }
    }
  }

  post("/output") {
    authenticated {
      getExecution { ex ⇒ Ok(Output(ex.workDirectory.readOutput).toJson) }
    }
  }

  post("/state") {
    authenticated {
      getExecution { ex ⇒
        val moleExecution = ex.moleExecution
        val state = (moleExecution.exception, moleExecution.finished) match {
          case (Some(t), _) ⇒ Failed(Error(t))
          case (None, true) ⇒ Finished()
          case _            ⇒ Running(moleExecution.ready, moleExecution.running, moleExecution.completed)

        }
        Ok(state.toJson)
      }
    }
  }

  post("/remove") {
    authenticated {
      getId {
        moles.remove(_) match {
          case None ⇒ ExpectationFailed(Error("Execution not found").toJson)
          case Some(ex) ⇒
            ex.moleExecution.cancel
            ex.workDirectory.clean
            Ok()
        }
      }
    }
  }

  post("/list") {
    authenticated {
      Ok(moles.getKeys.toSeq.toJson)
    }
  }

  def getExecution(success: Execution ⇒ ActionResult)(implicit r: HttpServletRequest): ActionResult =
    getId {
      moles.get(_) match {
        case None     ⇒ ExpectationFailed(Error("Execution not found").toJson)
        case Some(ex) ⇒ success(ex)
      }
    }(r)

  def getId(success: ExecutionId ⇒ ActionResult)(implicit r: HttpServletRequest): ActionResult =
    Try(params("id")(r)) match {
      case Failure(_)  ⇒ ExpectationFailed(Error("id is missing").toJson)
      case Success(id) ⇒ success(ExecutionId(id))
    }

  def authenticated[T](success: ⇒ ActionResult)(implicit r: HttpServletRequest): ActionResult = {
    def fail = Unauthorized(Error("This service requires a token").toJson)

    Try(params("token")(r)) match {
      case Failure(_) ⇒ fail
      case Success(k) ⇒ if (checkToken(k)) success else fail
    }
  }

}
