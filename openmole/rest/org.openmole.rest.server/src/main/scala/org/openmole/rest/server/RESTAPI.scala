package org.openmole.rest.server
//
//import java.io.{ File, PrintStream }
//import java.util.UUID
//import java.util.zip.GZIPInputStream
//
//import javax.servlet.annotation.MultipartConfig
//import javax.servlet.http.HttpServletRequest

//import org.openmole.core.event._
//import org.openmole.core.project._
//import org.openmole.core.workflow.execution.Environment
//import org.openmole.core.workflow.mole.{ MoleExecution, MoleExecutionContext, MoleServices }
//import org.openmole.core.dsl._
//import org.openmole.core.fileservice.FileServiceCache
//import org.openmole.core.services.Services
//import org.openmole.core.workspace.TmpDirectory
//
//import org.openmole.tool.collection._
//import org.openmole.tool.outputredirection.OutputRedirection
//import org.openmole.tool.stream._
//import org.openmole.tool.archive.*
//import org.scalatra._
//import org.scalatra.servlet.FileUploadSupport
//
//import scala.util.{ Failure, Success, Try }
import cats.effect.IO
import org.http4s
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.*
import org.http4s.implicits.*
import org.http4s.multipart.Multipart
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.json4s.*
import org.json4s.jackson.JsonMethods.*
import org.openmole.core.fileservice.FileServiceCache
import org.openmole.core.project.*
import org.openmole.core.workflow.mole.MoleServices

import java.io.PrintStream
import org.openmole.rest.message.*

import scala.util.{Failure, Success, Try}
import java.util.UUID
import java.util.zip.GZIPInputStream
import org.openmole.tool.stream.*
import org.openmole.tool.archive.*
import org.openmole.gui.server.ext

case class EnvironmentException(environment: Environment, error: Error)

case class Execution(
  jobDirectory:  JobDirectory,
  moleExecution: MoleExecution
)

case class Plugin(name: String, active: Boolean)
case class PluginError(name: String, error: Error)

case class JobDirectory(jobDirectory: File):
  val output = jobDirectory.newFile("output", ".txt")
  lazy val outputStream = new PrintStream(output.bufferedOutputStream())

  def tmpDirectory = jobDirectory /> "tmp"
  def workDirectory = jobDirectory /> "workDirectory"

  def readOutput =
    outputStream.flush
    output.content

  def clean =
    outputStream.close
    jobDirectory.recursiveDelete


class RESTAPI(services: Services):
  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

  private val logger = Log.log
  private lazy val moles = collection.concurrent.TrieMap[ExecutionId, Execution]()

  implicit class ToJsonDecorator(x: Any):
    def toJson = pretty(Extraction.decompose(x))

  lazy val baseDirectory = services.workspace.tmpDirectory.newDirectory("rest")
  def exceptionToHttpError(e: Throwable) = InternalServerError(Error(e).toJson)

  val routes: HttpRoutes[IO] =
    HttpRoutes.of:
      case req @ POST -> Root / "job" =>
        req.decode[Multipart[IO]]: parts =>
          import cats.effect.unsafe.implicits.global
          def scriptValue = ext.utils.multipartStringContent(parts, "script")
          def workDirectoryValue = ext.utils.multipartContent(parts, "workDirectory")

          (scriptValue, workDirectoryValue) match
            case (_, None) ⇒ ExpectationFailed(Error("Missing mandatory workDirectory parameter.").toJson)
            case (None, _) ⇒ ExpectationFailed(Error("Missing mandatory script parameter.").toJson)
            case (Some(script), Some(archive)) ⇒
              logger.info("starting the create operation")

              val id = ExecutionId(UUID.randomUUID().toString)
              val directory = JobDirectory(baseDirectory / id.id)

              val stream = fs2.io.toInputStreamResource(archive.body)
              stream.use { st =>
                IO:
                  val is = new TarInputStream(new GZIPInputStream(st))
                  try is.extract(directory.workDirectory)
                  finally is.close()
              }.unsafeRunSync()

              def error(e: Throwable) =
                directory.clean
                ExpectationFailed(Error(e).toJson)

              def start(ex: MoleExecution) =
                Try(ex.start(true)) match
                  case Failure(e) ⇒ error(e)
                  case Success(ex) ⇒
                    moles.put(id, Execution(directory, ex))
                    Ok(id.toJson)

              val jobServices =
                import services._

                Services.copy(services)(
                  outputRedirection = OutputRedirection(directory.outputStream),
                  newFile = TmpDirectory(directory.tmpDirectory),
                  fileServiceCache = FileServiceCache()
                )

              Project.compile(directory.workDirectory, directory.workDirectory / script)(jobServices) match
                case ScriptFileDoesNotExists() ⇒
                  def content = directory.workDirectory.listFiles().map(_.getName).mkString(", ")
                  ExpectationFailed(Error(s"The script doesn't exist in the workDirectory. The content of the workDirectory is: $content").toJson)
                case e: CompilationError       ⇒ error(e.error)
                case compiled: Compiled ⇒
                  Try(compiled.eval(Seq.empty)(jobServices)) match
                    case Success(res) ⇒
                      import jobServices._

                      val moleServices =
                        MoleServices.create(
                          applicationExecutionDirectory = baseDirectory,
                          moleExecutionDirectory = Some(directory.tmpDirectory),
                          outputRedirection = Some(OutputRedirection(directory.outputStream)),
                          compilationContext = Some(compiled.compilationContext)
                        )
                      Try:
                        DSL.toPuzzle(res).toExecution()(moleServices)
                      match
                        case Success(ex) ⇒ start(ex)
                        case Failure(e) ⇒
                          MoleServices.clean(moleServices)
                          error(e)
                    case Failure(e) ⇒ error(e)
      case req @ GET -> "job" /: rest if rest.segments.size > 1 && rest.segments(1).decoded() == "workDirectory" =>
        val id = rest.segments(0).decoded()
        val path = rest.segments.drop(2).map(_.decoded()).mkString("/")

        getExecution(ExecutionId(id)): ex ⇒
          val file = ex.jobDirectory.workDirectory / path

          if !file.exists()
          then NotFound(Error("File not found").toJson)
          else
            if file.isDirectory
            then
              ext.utils.sendFileStream(s"${file.getName}.tgz"): out =>
                val tos = new TarOutputStream(out.toGZ, 64 * 1024)
                try tos.archive(file)
                finally tos.close()
            else
              ext.utils.sendFile(req, file)

  def getExecution[T](id: ExecutionId)(success: Execution ⇒ T) =
    moles.get(id) match
      case None     ⇒ NotFound(Error("Execution not found").toJson)
      case Some(ex) ⇒ success(ex)




//
//@MultipartConfig(fileSizeThreshold = 1024 * 1024) //research scala multipart config
//trait RESTAPI extends ScalatraServlet
//  with ContentEncodingSupport
//  with FileUploadSupport
//  with FlashMapSupport {
//
//  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal
//
//  private val logger = Log.log
//  private lazy val moles = DataHandler[ExecutionId, Execution]()
//
//  implicit class ToJsonDecorator(x: Any) {
//    def toJson = pretty(Extraction.decompose(x))
//  }
//
//  val arguments: RESTLifeCycle.Arguments
//  val services = arguments.services
//
//  lazy val baseDirectory = services.workspace.tmpDirectory.newDirectory("rest")
//  def exceptionToHttpError(e: Throwable) = InternalServerError(Error(e).toJson)
//
//  post("/job") {
//    (params get "script", fileParams get "workDirectory") match {
//      case (_, None) ⇒ ExpectationFailed(Error("Missing mandatory workDirectory parameter.").toJson)
//      case (None, _) ⇒ ExpectationFailed(Error("Missing mandatory script parameter.").toJson)
//      case (Some(script), Some(archive)) ⇒
//        logger.info("starting the create operation")
//
//        val id = ExecutionId(UUID.randomUUID().toString)
//        val directory = JobDirectory(baseDirectory / id.id)
//
//        val is = new TarInputStream(new GZIPInputStream(archive.getInputStream))
//        try is.extract(directory.workDirectory) finally is.close
//
//        def error(e: Throwable) = {
//          directory.clean
//          ExpectationFailed(Error(e).toJson)
//        }
//
//        def start(ex: MoleExecution) =
//          Try(ex.start(true)) match {
//            case Failure(e) ⇒ error(e)
//            case Success(ex) ⇒
//              moles.add(id, Execution(directory, ex))
//              Ok(id.toJson)
//          }
//
//        val jobServices = {
//          import services._
//
//          Services.copy(services)(
//            outputRedirection = OutputRedirection(directory.outputStream),
//            newFile = TmpDirectory(directory.tmpDirectory),
//            fileServiceCache = FileServiceCache()
//          )
//        }
//
//        Project.compile(directory.workDirectory, directory.workDirectory / script)(jobServices) match {
//          case ScriptFileDoesNotExists() ⇒ ExpectationFailed(Error("The script doesn't exist").toJson)
//          case e: CompilationError       ⇒ error(e.error)
//          case compiled: Compiled ⇒
//            Try(compiled.eval(Seq.empty)(jobServices)) match {
//              case Success(res) ⇒
//                import jobServices._
//
//                val moleServices =
//                  MoleServices.create(
//                    applicationExecutionDirectory = baseDirectory,
//                    moleExecutionDirectory = Some(directory.tmpDirectory),
//                    outputRedirection = Some(OutputRedirection(directory.outputStream)),
//                    compilationContext = Some(compiled.compilationContext)
//                  )
//                Try {
//                  DSL.toPuzzle(res).toExecution()(moleServices)
//                } match {
//                  case Success(ex) ⇒ start(ex)
//                  case Failure(e) ⇒
//                    MoleServices.clean(moleServices)
//                    error(e)
//                }
//              case Failure(e) ⇒ error(e)
//            }
//        }
//    }
//  }
//
//  get("/job/:id/workDirectory/*") {
//    getExecution { ex ⇒
//      val path = multiParams("splat").headOption.getOrElse("") //(params get "path").getOrElse("")
//      val file = ex.jobDirectory.workDirectory / path
//
//      if (!file.exists()) NotFound(Error("File not found").toJson)
//      else {
//        val gzOs = response.getOutputStream.toGZ
//
//        try {
//          if (file.isDirectory) {
//            val os = new TarOutputStream(gzOs)
//            contentType = "application/octet-stream"
//            response.setHeader("Content-Disposition", "attachment; filename=" + "archive.tgz")
//            os.archive(file)
//            os.close
//          }
//          else {
//            contentType = "application/octet-stream"
//            response.setHeader("Content-Disposition", "attachment; filename=" + file.getName + ".gz")
//            file.copy(gzOs)
//          }
//        }
//        finally gzOs.flushClose
//
//        Ok()
//      }
//    }
//  }
//
//  propfind("/job/:id/workDirectory/*") {
//    getExecution { ex ⇒
//      val pathParam = multiParams("splat").headOption //params get "path"
//      val path = pathParam.getOrElse("")
//      val file = ex.jobDirectory.workDirectory / path
//
//      if (!file.exists()) NotFound(Error("File not found").toJson)
//      else if (file.isDirectory) {
//
//        def filter(fs: Array[File]) =
//          params get "last" match {
//            case Some(l) ⇒ fs.sortBy(-_.lastModified).take(l.toInt)
//            case None    ⇒ fs.sortBy(-_.lastModified)
//          }
//
//        val entries =
//          filter(file.listFilesSafe).toVector.map { f ⇒
//            val size = if (f.isFile) Some(f.size) else None
//            val entryType = if (f.isDirectory) FileType.directory else FileType.file
//            DirectoryEntryProperty(f.getName, modified = f.lastModified(), size = size, `type` = entryType)
//          }
//
//        Ok(DirectoryProperty(entries, modified = file.lastModified()).toJson)
//      }
//      else Ok(FileProperty(file.size, modified = file.lastModified()).toJson)
//    }
//  }
//
//  get("/job/:id/output") {
//    getExecution { ex ⇒ Ok(ex.jobDirectory.readOutput) }
//  }
//
//  get("/job/:id/state") {
//    getExecution { ex ⇒
//      val moleExecution = ex.moleExecution
//      val state: State = (moleExecution.exception, moleExecution.finished) match {
//        case (Some(t), _) ⇒
//          MoleExecution.MoleExecutionFailed.capsule(t) match {
//            case Some(c) ⇒ Failed(Error(t.exception).copy(message = s"Mole execution failed when executing capsule: ${c}"))
//            case None    ⇒ Failed(Error(t.exception).copy(message = s"Mole execution failed"))
//          }
//
//        case (None, true) ⇒ Finished()
//        case _ ⇒
//          def environments = moleExecution.environments.values.toSeq ++ Seq(moleExecution.defaultEnvironment)
//          def environmentStatus = environments.map {
//            env ⇒
//              def environmentErrors = Environment.clearErrors(env).map(e ⇒ Error(e.exception).copy(level = Some(e.level.toString)))
//              EnvironmentStatus(name = env.name, submitted = env.submitted, running = env.running, done = env.done, failed = env.failed, environmentErrors)
//          }
//          val statuses = moleExecution.capsuleStatuses
//          val capsuleStates = statuses.toVector.map { case (c, states) ⇒ c.toString -> CapsuleState(states.ready, states.running, states.completed) }
//          val ready = capsuleStates.map(_._2.ready).sum
//          val running = capsuleStates.map(_._2.running).sum
//          val completed = capsuleStates.map(_._2.completed).sum
//
//          Running(ready, running, completed, capsuleStates, environmentStatus)
//      }
//      Ok(state.toJson)
//    }
//  }
//
//  delete("/job/:id") {
//    getId {
//      moles.remove(_) match {
//        case None ⇒ NotFound(Error("Execution not found").toJson)
//        case Some(ex) ⇒
//          ex.moleExecution.cancel
//          ex.jobDirectory.clean
//          Ok()
//      }
//    }
//  }
//
//  get("/job/") {
//    Ok(moles.getKeys.toSeq.toJson)
//  }
//
//  /* --------------- Plugin API ----------- */
//
//  get("/plugin/") {
//    import services._
//    val plugins =
//      org.openmole.core.module.pluginDirectory.listFilesSafe.map { p ⇒
//        Plugin(
//          p.getName,
//          active = org.openmole.core.pluginmanager.PluginManager.bundle(p).isDefined
//        )
//      }
//
//    Ok(plugins.toSeq.toJson)
//  }
//
//  post("/plugin") {
//    import services._
//    (fileMultiParams get "file") match {
//      case None ⇒ ExpectationFailed(Error("Missing mandatory file parameter.").toJson)
//      case Some(files) ⇒
//        val extractDirectory = baseDirectory.newDirectory("plugins")
//        extractDirectory.mkdirs()
//
//        val (plugins, errors) =
//          try {
//            val plugins =
//              for {
//                file ← files
//              } yield {
//                val plugin = extractDirectory / file.name
//                file.getInputStream.copy(plugin)
//                plugin
//              }
//
//            (plugins.map(_.getName), org.openmole.core.module.addPluginsFiles(plugins, true, org.openmole.core.module.pluginDirectory))
//          }
//          finally extractDirectory.recursiveDelete
//
//        if (!errors.isEmpty) errors.map(e ⇒ PluginError(e._1.getName, Error(e._2))).toJson
//        else Ok()
//    }
//  }
//
//  delete("/plugin") {
//    import services._
//    (multiParams get "name") match {
//      case None ⇒ ExpectationFailed(Error("Missing mandatory name parameter.").toJson)
//      case Some(names) ⇒
//        import org.openmole.core.pluginmanager._
//
//        val allNames =
//          for {
//            name ← names
//          } yield {
//            val file = org.openmole.core.module.pluginDirectory / name
//            val allDependingFiles = PluginManager.allDepending(file, b ⇒ !b.isProvided)
//
//            val allNames = allDependingFiles.map(_.getName)
//            val bundle = PluginManager.bundle(file)
//
//            bundle.foreach(PluginManager.remove)
//            allDependingFiles.filter(f ⇒ !PluginManager.bundle(f).isDefined).foreach(_.recursiveDelete)
//            file.recursiveDelete
//            allNames
//          }
//
//        Ok(allNames.flatten.toJson)
//    }
//  }
//
//  def getExecution(success: Execution ⇒ ActionResult)(implicit r: HttpServletRequest): ActionResult =
//    getId {
//      moles.get(_) match {
//        case None     ⇒ NotFound(Error("Execution not found").toJson)
//        case Some(ex) ⇒ success(ex)
//      }
//    }(r)
//
//  def getId(success: ExecutionId ⇒ ActionResult)(implicit r: HttpServletRequest): ActionResult =
//    Try(params("id")(r)) match {
//      case Failure(_)  ⇒ ExpectationFailed(Error("id is missing").toJson)
//      case Success(id) ⇒ success(ExecutionId(id))
//    }
//
//  def propfind(transformers: RouteTransformer*)(action: ⇒ Any): Route = addRoute(HttpMethod("PROPFIND"), transformers, action)
//
//}
