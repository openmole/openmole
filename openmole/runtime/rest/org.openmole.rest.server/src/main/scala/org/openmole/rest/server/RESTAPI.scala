package org.openmole.rest.server

import java.io.{ File, PrintStream }
import java.util.UUID
import java.util.logging.Level
import java.util.zip.{ GZIPOutputStream, GZIPInputStream }
import javax.servlet.annotation.MultipartConfig
import javax.servlet.http.HttpServletRequest
import org.json4s.JsonDSL._
import org.json4s._
import org.openmole.console._
import org.openmole.core.event._
import org.openmole.core.workflow.execution.Environment
import org.openmole.core.workflow.execution.Environment.ExceptionRaised
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
import sun.awt.EventListenerAggregate
import scala.util.{ Try, Failure, Success }
import org.openmole.tool.collection._

case class EnvironmentException(environment: Environment, error: Error)

case class Execution(
  workDirectory: WorkDirectory,
  moleExecution: MoleExecution,
  environmentErrors: EventAccumulator[EnvironmentException])

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
  def baseDirectory = Workspace.location / "rest"

  def exceptionToHttpError(e: Throwable) = InternalServerError(Error(e).toJson)

  post("/token") {
    Try(params("password")) map issueToken match {
      case Failure(_) ⇒ ExpectationFailed(Error("No password sent with request").toJson)
      case Success(Failure(InvalidPasswordException(msg))) ⇒ Forbidden(Error(msg).toJson)
      case Success(Failure(e)) ⇒ exceptionToHttpError(e)
      case Success(Success(AuthenticationToken(token, start, end))) ⇒ Accepted(Token(token, end - start).toJson)
    }
  }

  post("/start") {
    authenticate()
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

        def error(e: Throwable) = {
          directory.clean
          ExpectationFailed(Error(e).toJson)
        }

        def compile = {
          val console = new Console(arguments.plugins)
          val repl = console.newREPL(ConsoleVariables(workDirectory = baseDirectory)(inputDirectory = directory.inputDirectory, outputDirectory = directory.outputDirectory))
          repl.eval(script)
        }

        def start(ex: MoleExecution) = {

          val accumulator =
            EventAccumulator(ex.environments.values.toSeq: _*) {
              case (env, ev: ExceptionRaised) ⇒ EnvironmentException(env, Error(ev.exception).copy(level = Some(ev.level.getName)))
            }
          Try(ex.start) match {
            case Failure(e) ⇒ error(e)
            case Success(ex) ⇒
              moles.add(id, Execution(directory, ex, accumulator))
              Ok(id.toJson)
          }
        }

        def launch: ActionResult =
          Try(compile) match {
            case Failure(e) ⇒ error(e)
            case Success(o) ⇒
              o match {
                case puzzle: Puzzle ⇒
                  Try(puzzle.toExecution(executionContext = ExecutionContext(out = directory.outputStream))) match {
                    case Success(ex) ⇒
                      ex listen { case (ex, ev: MoleExecution.Finished) ⇒ }
                      start(ex)
                    case Failure(e) ⇒ error(e)
                  }
                case _ ⇒
                  directory.clean
                  ExpectationFailed(Error("The last line of the script should be a puzzle").toJson)
              }
          }

        extract
        launch
    }

  }

  post("/outputDirectory") {
    authenticate()
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

  post("/output") {
    authenticate()
    getExecution { ex ⇒ Ok(Output(ex.workDirectory.readOutput).toJson) }
  }

  post("/state") {
    authenticate()
    getExecution { ex ⇒
      val moleExecution = ex.moleExecution
      val state: State = (moleExecution.exception, moleExecution.finished) match {
        case (Some(t), _) ⇒ Failed(Error(t.exception).copy(message = s"Mole execution failed when execution capsule: ${t.capsule}"))
        case (None, true) ⇒ Finished()
        case _ ⇒
          def environments = moleExecution.environments.values.toSeq
          def environmentStatus = environments.map {
            env ⇒
              def environmentErrors = ex.environmentErrors.clear.filter(_.environment == env).map(_.error)
              EnvironmentStatus(name = env.name, submitted = env.submitted, running = env.running, done = env.done, failed = env.failed, environmentErrors)
          }
          Running(moleExecution.ready, moleExecution.running, moleExecution.completed, environmentStatus)
      }
      Ok(state.toJson)
    }
  }

  post("/remove") {
    authenticate()
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

  post("/list") {
    authenticate()
    Ok(moles.getKeys.toSeq.toJson)
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

  def authenticate()(implicit r: HttpServletRequest) = {
    def fail = halt(401, Error("This service requires a valid token").toJson)

    Try(params("token")(r)) match {
      case Failure(_) ⇒ fail
      case Success(k) ⇒ if (!checkToken(k)) fail
    }
  }

}
