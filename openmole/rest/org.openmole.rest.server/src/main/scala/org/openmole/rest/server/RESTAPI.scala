package org.openmole.rest.server

import cats.effect.IO
import org.http4s
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.*
import org.http4s.implicits.*
import org.http4s.multipart.Multipart
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.fileservice.FileServiceCache
import org.openmole.core.format.OMRFormat
import org.openmole.core.project.*
import org.openmole.core.workflow.mole.MoleServices

import java.io.PrintStream
import org.openmole.rest.message.*

import scala.util.{Failure, Success, Try}
import java.util.UUID
import java.util.zip.GZIPInputStream
import org.openmole.tool.stream.*
import org.openmole.tool.archive.*
import org.openmole.gui.server.ext.utils.HTTP

case class EnvironmentException(environment: Environment, error: Error)

case class Execution(
  jobDirectory:  JobDirectory,
  moleExecution: MoleExecution
)


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
  // protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

  private val logger = Log.log
  private lazy val moles = collection.concurrent.TrieMap[ExecutionId, Execution]()

  implicit class ToJsonDecorator[T: io.circe.Encoder](x: T):
    def toJson =
      import io.circe.*
      import io.circe.syntax.*
      x.asJson.deepDropNullValues.spaces2 //pretty(Extraction.decompose(x))

  lazy val baseDirectory = services.workspace.tmpDirectory.newDirectory("rest")
  def exceptionToHttpError(e: Throwable) = InternalServerError(Error(e).toJson)

  val routes: HttpRoutes[IO] =
    HttpRoutes.of:
      case req @ POST -> Root / "job" =>
        req.decode[Multipart[IO]]: parts =>
          import cats.effect.unsafe.implicits.global
          def scriptValue = HTTP.multipartStringContent(parts, "script")
          def workDirectoryValue = HTTP.multipartContent(parts, "workDirectory")

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
                  val is = TarArchiveInputStream(new GZIPInputStream(st))
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
                        MoleExecution(res)(moleServices)
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
              HTTP.sendFileStream(s"${file.getName}.tgz"): out =>
                val tos = TarArchiveOutputStream(out.toGZ, Some(64 * 1024))
                try tos.archive(file)
                finally tos.close()
            else
              HTTP.sendFile(req, file)
      case req @ Method.PROPFIND -> "job" /: rest if rest.segments.size > 1 && rest.segments(1).decoded() == "workDirectory" =>
        import io.circe.generic.semiauto.*
        val id = rest.segments(0).decoded()
        val path = rest.segments.drop(2).map(_.decoded()).mkString("/")

        getExecution(ExecutionId(id)): ex ⇒
          val file = ex.jobDirectory.workDirectory / path

          if !file.exists()
          then NotFound(Error("File not found").toJson)
          else
            if file.isDirectory
            then
              def filter(fs: Array[File]) =
                req.params.get("last") match
                  case Some(l) ⇒ fs.sortBy(-_.lastModified).take(l.toInt)
                  case None    ⇒ fs.sortBy(-_.lastModified)

              val entries =
                filter(file.listFilesSafe).toVector.map: f ⇒
                  val size = if (f.isFile) Some(f.size) else None
                  val entryType = if (f.isDirectory) FileType.directory else FileType.file
                  DirectoryEntry(f.getName, modified = f.lastModified(), size = size, `type` = entryType)

              def property = DirectoryProperty(entries, modified = file.lastModified())
              Ok(property.toJson)
            else
              def property = FileProperty(file.size, modified = file.lastModified())
              Ok(property.toJson)
      case req@ GET -> "job" /: rest if rest.segments.size > 1 && rest.segments(1).decoded() == "omrToCSV" =>
        import services.*
        val id = rest.segments(0).decoded()
        val path = rest.segments.drop(2).map(_.decoded()).mkString("/")

        getExecution(ExecutionId(id)): ex ⇒
          val file = ex.jobDirectory.workDirectory / path
          checkIsOMR(file):
            HTTP.convertOMR(req, file, org.openmole.gui.shared.data.GUIOMRContent.ExportFormat.CSV, history = false)
      case req@ GET -> "job" /: rest if rest.segments.size > 1 && rest.segments(1).decoded() == "omrToJSON" =>
        import services.*
        val id = rest.segments(0).decoded()
        val path = rest.segments.drop(2).map(_.decoded()).mkString("/")

        getExecution(ExecutionId(id)): ex ⇒
          val file = ex.jobDirectory.workDirectory / path
          checkIsOMR(file):
            HTTP.convertOMR(req, file, org.openmole.gui.shared.data.GUIOMRContent.ExportFormat.JSON, history = false)
      case req@GET -> "job" /: rest if rest.segments.size > 1 && rest.segments(1).decoded() == "omrFiles" =>
        import services.*
        val id = rest.segments(0).decoded()
        val path = rest.segments.drop(2).map(_.decoded()).mkString("/")

        getExecution(ExecutionId(id)): ex ⇒
          val file = ex.jobDirectory.workDirectory / path

          checkIsOMR(file):
            OMRFormat.resultFileDirectory(file) match
              case Some(fileDirectory) =>
                HTTP.sendFileStream(s"${file.baseName}-files.tgz"): out =>
                  val tos = TarArchiveOutputStream(out.toGZ, Some(64 * 1024))
                  try tos.archive(fileDirectory, includeTopDirectoryName = false)
                  finally tos.close()
              case None =>
                ExpectationFailed(Error(s"The OMR file does not contain files").toJson)
      case req @ GET -> Root / "job" / id / "output" =>
        getExecution(ExecutionId(id)): ex ⇒
          Ok(ex.jobDirectory.readOutput)
      case req @ GET -> Root / "job" / id / "state" =>
        getExecution(ExecutionId(id)): ex ⇒
          val moleExecution = ex.moleExecution
          val state =
            (moleExecution.exception, moleExecution.finished) match
              case (Some(t), _) ⇒
                MoleExecution.MoleExecutionFailed.capsule(t) match
                  case Some(c) ⇒ Failed(Error(t.exception).copy(message = s"Mole execution failed when executing capsule: ${c}")).toJson
                  case None    ⇒ Failed(Error(t.exception).copy(message = s"Mole execution failed")).toJson

              case (None, true) ⇒ Finished().toJson
              case _ ⇒
                def environments = moleExecution.environments ++ Seq(moleExecution.defaultEnvironment)
                def environmentStatus = environments.map: env =>
                  def environmentErrors = Environment.clearErrors(env).map(e ⇒ Error(e.exception).copy(level = Some(e.level.toString)))
                  EnvironmentStatus(name = env.name, submitted = env.submitted, running = env.running, done = env.done, failed = env.failed, environmentErrors)

                val statuses = moleExecution.capsuleStatuses
                val capsuleStates = statuses.toVector.map { (c, states) ⇒ c.toString -> CapsuleState(states.ready, states.running, states.completed) }
                val ready = capsuleStates.map(_._2.ready).sum
                val running = capsuleStates.map(_._2.running).sum
                val completed = capsuleStates.map(_._2.completed).sum

                Running(ready, running, completed, capsuleStates, environmentStatus).toJson

          Ok(state)
      case req @ DELETE -> Root / "job" / id =>
        moles.remove(ExecutionId(id)) match
          case None ⇒ NotFound(Error("Execution not found").toJson)
          case Some(ex) ⇒
            ex.moleExecution.cancel
            ex.jobDirectory.clean
            Ok()
      case req @ GET -> Root / "job" =>
        Ok(moles.keys.toSeq.toJson)

      /* --------------- Plugin API ----------- */

      case req @ GET -> Root / "plugin" =>
        import services._
        val plugins =
          org.openmole.core.module.pluginDirectory.listFilesSafe.map: p ⇒
            Plugin(
              p.getName,
              active = org.openmole.core.pluginmanager.PluginManager.bundle(p).isDefined
            )
        Ok(plugins.toSeq.toJson)

      case req @ POST -> Root / "plugin" =>
        req.decode[Multipart[IO]]: parts =>
          import cats.effect.unsafe.implicits.global
          import services._
          def fileValue = HTTP.listMultipartContent(parts, "file", shouldBeFile = true)
          fileValue match
            case Seq() ⇒ ExpectationFailed(Error("Missing mandatory file parameter.").toJson)
            case files ⇒
              val extractDirectory = baseDirectory.newDirectory("plugins")
              extractDirectory.mkdirs()

              val (plugins, errors) =
                try
                  val plugins =
                    for
                      file ← files
                    yield
                      val plugin = extractDirectory / file.filename.get
                      val stream = fs2.io.toInputStreamResource(file.body)
                      stream.use { st =>
                        IO:
                          st copy plugin
                      }.unsafeRunSync()
                      plugin

                  (plugins.map(_.getName), org.openmole.core.module.addPluginsFiles(plugins, true, org.openmole.core.module.pluginDirectory))
                finally extractDirectory.recursiveDelete

              if errors.nonEmpty
              then InternalServerError(errors.map(e ⇒ PluginError(e._1.getName, Error(e._2))).toJson)
              else Ok()
      case req @ DELETE -> Root / "plugin" =>
        import services._
        req.multiParams.get("name") match
          case None ⇒ ExpectationFailed(Error("Missing mandatory name parameter.").toJson)
          case Some(names) ⇒
            import org.openmole.core.pluginmanager._

            val allNames =
              for
                name ← names
              yield
                val file = org.openmole.core.module.pluginDirectory / name
                val allDependingFiles = PluginManager.allDepending(file, b ⇒ !b.isProvided)

                val allNames = allDependingFiles.map(_.getName)
                val bundle = PluginManager.bundle(file)

                bundle.foreach(PluginManager.remove)
                allDependingFiles.filter(f ⇒ !PluginManager.bundle(f).isDefined).foreach(_.recursiveDelete)
                file.recursiveDelete
                allNames

            Ok(allNames.flatten.toJson)



  def getExecution[T](id: ExecutionId)(success: Execution ⇒ T) =
    moles.get(id) match
      case None     ⇒ NotFound(Error("Execution not found").toJson)
      case Some(ex) ⇒ success(ex)

  def checkIsOMR[T](file: File)(f: => T) =
    if !file.exists()
    then NotFound(Error("File not found").toJson)
    else if !org.openmole.core.format.OMRFormat.isOMR(file)
    then ExpectationFailed(Error("File is not an \".omr\" file").toJson)
    else f
