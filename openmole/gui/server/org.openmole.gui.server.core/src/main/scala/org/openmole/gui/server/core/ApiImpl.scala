package org.openmole.gui.server.core

import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.zip.GZIPInputStream

import org.openmole.plugin.environment.batch.environment.BatchEnvironment.{ BeginDownload, BeginUpload, EndDownload, EndUpload }
import org.openmole.core.buildinfo
import org.openmole.core.event._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.pluginmanager._
import org.openmole.gui.server.core.Utils._
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.gui.ext.data
import org.openmole.gui.ext.data._
import java.io._
import java.nio.file._
import java.util.concurrent.atomic.AtomicReference

import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.fileservice.FileService
import org.openmole.core.market.{ MarketIndex, MarketIndexEntry }

import scala.util.{ Failure, Success, Try }
import org.openmole.core.workflow.mole.MoleExecutionContext
import org.openmole.tool.stream.StringPrintStream

import scala.concurrent.stm._
import org.openmole.tool.file._
import org.openmole.tool.tar._
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.module
import org.openmole.core.market
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.project._
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.SerializerService
import org.openmole.core.services.Services
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.plugin.server._
import org.openmole.gui.ext.tool.server.OMRouter
import org.openmole.gui.ext.tool.server.Utils.authenticationKeysFile
import org.openmole.gui.server.core.GUIServer.ApplicationControl
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.random.{ RandomProvider, Seeder }

/*
 * Copyright (C) 21/07/14 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

class ApiImpl(s: Services, applicationControl: ApplicationControl) extends Api {

  implicit def services = s
  import s._

  val outputSize = ConfigurationLocation[Int]("gui", "outputsize", Some(10 * 1024 * 1024))

  val execution = new Execution

  //GENERAL
  def settings: OMSettings = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    OMSettings(
      Utils.projectsDirectory(),
      buildinfo.version.value,
      buildinfo.name,
      new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(buildinfo.BuildInfo.buildTime)
    )
  }

  def shutdown = applicationControl.stop()

  def restart = applicationControl.restart()

  def isAlive = true

  def jvmInfos = {
    val runtime = Runtime.getRuntime
    val totalMemory = runtime.totalMemory
    val allocatedMemory = totalMemory - runtime.freeMemory
    val javaVersion = System.getProperty("java.version")
    val jvmName = System.getProperty("java.vm.name")

    JVMInfos(
      javaVersion,
      jvmName,
      Runtime.getRuntime.availableProcessors,
      allocatedMemory,
      totalMemory
    )
  }

  //AUTHENTICATIONS
  def renameKey(keyName: String, newName: String): Unit =
    Files.move(new File(authenticationKeysFile, keyName).toPath, new File(authenticationKeysFile, newName).toPath, StandardCopyOption.REPLACE_EXISTING)

  //WORKSPACE
  def isPasswordCorrect(pass: String): Boolean = Preference.passwordIsCorrect(Cypher(pass), services.preference)

  //def passwordState = Utils.passwordState

  def resetPassword(): Unit = Services.resetPassword

  // FILES
  def addDirectory(safePath: SafePath, directoryName: String): Boolean = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    new File(safePath, directoryName).mkdirs
  }

  def addFile(safePath: SafePath, fileName: String): Boolean = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    new File(safePath, fileName).createNewFile
  }

  def deleteFile(safePath: SafePath, context: ServerFileSystemContext): Unit = Utils.deleteFile(safePath, context)

  def deleteFiles(safePaths: Seq[SafePath], context: ServerFileSystemContext): Unit = Utils.deleteFiles(safePaths, context)

  private def getExtractedArchiveTo(from: File, to: File)(implicit context: ServerFileSystemContext): Seq[SafePath] = {
    extractArchiveFromFiles(from, to)
    to.listFiles.toSeq
  }

  def unknownFormat(name: String) = ExtractResult(Some(ErrorBuilder("Unknown compression format for " + name)))

  private def extractArchiveFromFiles(from: File, to: File)(implicit context: ServerFileSystemContext): ExtractResult = {
    Try {
      val ext = DataUtils.fileToExtension(from.getName)
      ext match {
        case org.openmole.gui.ext.data.Tar ⇒
          from.extract(to)
          to.applyRecursive((f: File) ⇒ f.setWritable(true))
        case TarGz ⇒
          from.extractUncompress(to, true)
          to.applyRecursive((f: File) ⇒ f.setWritable(true))
        case Zip ⇒ Utils.unzip(from, to)
        case _   ⇒ throw new Throwable("Unknown compression format for " + from.getName)
      }
    } match {
      case Success(_) ⇒ ExtractResult.ok
      case Failure(t) ⇒ ExtractResult(Some(ErrorBuilder(t)))
    }
  }

  def extractTGZ(safePath: SafePath): ExtractResult = {
    DataUtils.fileToExtension(safePath.name) match {
      case FileExtension.TGZ | FileExtension.TAR | FileExtension.ZIP ⇒
        val archiveFile = safePathToFile(safePath)(ServerFileSystemContext.project, workspace)
        val toFile: File = safePathToFile(safePath.parent)(ServerFileSystemContext.project, workspace)
        extractArchiveFromFiles(archiveFile, toFile)(ServerFileSystemContext.project)
      case _ ⇒ unknownFormat(safePath.name)
    }
  }

  def temporaryFile(): SafePath = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.absolute
    val dir = services.newFile.newDir("openmoleGUI")
    dir.mkdirs()
    dir
  }

  def exists(safePath: SafePath): Boolean = Utils.exists(safePath)

  def existsExcept(exception: SafePath, exceptItSelf: Boolean): Boolean = Utils.existsExcept(exception, exceptItSelf)

  def copyFromTmp(tmpSafePath: SafePath, filesToBeMovedTo: Seq[SafePath]): Unit = Utils.copyFromTmp(tmpSafePath, filesToBeMovedTo)

  def copyAllTmpTo(tmpSafePath: SafePath, to: SafePath): Unit = Utils.copyAllTmpTo(tmpSafePath, to)

  def copyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath) = Utils.copyProjectFilesTo(safePaths, to)

  def copyToPluginUploadDir(safePaths: Seq[SafePath]): Unit = Utils.copyToPluginUploadDirectory(safePaths)

  def testExistenceAndCopyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Seq[SafePath] = Utils.testExistenceAndCopyProjectFilesTo(safePaths, to)

  // Test whether safePathToTest exists in "in"
  def extractAndTestExistence(safePathToTest: SafePath, in: SafePath): Seq[SafePath] = {

    // import org.openmole.gui.ext.data.ServerFileSystemContext.absolute

    def test(sps: Seq[SafePath], inDir: SafePath = in) = {
      import org.openmole.gui.ext.data.ServerFileSystemContext.absolute

      val toTest: Seq[SafePath] = if (sps.size == 1) sps.flatMap { f ⇒
        if (f.isDirectory) f.listFiles.map {
          fileToSafePath
        }
        else Seq(f)
      }
      else sps

      toTest.filter { sp ⇒
        exists(inDir ++ sp.name)
      }.map { sp ⇒ inDir ++ sp.name }
    }

    val fileType: FileType = safePathToTest
    fileType match {
      case a: Archive ⇒ a.language match {
        case j: JavaLikeLanguage ⇒ test(Seq(safePathToTest))
        case _ ⇒
          // val emptyFile = new File("")
          val from: File = safePathToFile(safePathToTest)(ServerFileSystemContext.absolute, workspace)
          val to: File = safePathToFile(safePathToTest.parent)(ServerFileSystemContext.absolute, workspace)
          val extracted = getExtractedArchiveTo(from, to)(ServerFileSystemContext.absolute).filterNot {
            _ == safePathToTest
          }
          val toTest = in ++ safePathToTest.nameWithNoExtension
          val toTestFile: File = safePathToFile(in ++ safePathToTest.nameWithNoExtension)(ServerFileSystemContext.project, workspace)
          new File(to, from.getName).recursiveDelete

          if (toTestFile.exists) {
            test(extracted, toTest)
          }
          else Seq()
      }
      case _ ⇒ test(Seq(safePathToTest))
    }
  }

  private def safePath(safePath: SafePath): SafePath = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    safePathToFile(safePath)
  }

  def safePath(safePaths: Seq[SafePath]): Seq[SafePath] = {
    safePaths.map {
      safePath
    }
  }

  def listFiles(sp: SafePath, fileFilter: data.FileFilter): ListFilesData = atomic { implicit ctx ⇒
    Utils.listFiles(sp, fileFilter)(org.openmole.gui.ext.data.ServerFileSystemContext.project, workspace)
  }

  def isEmpty(sp: SafePath): Boolean = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val f: File = safePathToFile(sp)
    f.isDirectoryEmpty
  }

  def move(from: SafePath, to: SafePath): Unit = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val fromFile = safePathToFile(from)
    val toFile = safePathToFile(to)
    Utils.move(fromFile, toFile)
  }

  def replicate(safePath: SafePath, newName: String): SafePath = Utils.replicate(safePath, newName)

  def mdToHtml(safePath: SafePath): String = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    MarkDownProcessor(safePathToFile(safePath).content)
  }

  def renameFile(safePath: SafePath, name: String): SafePath = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    val targetFile = new File(safePath.parent, name)

    Files.move(safePathToFile(safePath), targetFile, StandardCopyOption.REPLACE_EXISTING)
    targetFile
  }

  def saveFile(path: SafePath, fileContent: String): Unit = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    safePathToFile(path).content = fileContent
  }

  def saveFiles(fileContents: Seq[AlterableFileContent]): Unit = fileContents.foreach { fc ⇒
    saveFile(fc.path, fc.content)
  }

  def size(safePath: SafePath): Long = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    safePathToFile(safePath).length
  }

  // EXECUTIONS
  def cancelExecution(id: ExecutionId): Unit = execution.cancel(id)
  def removeExecution(id: ExecutionId): Unit = execution.remove(id)

  def runScript(scriptData: ScriptData): Unit = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    val execId = ExecutionId(getUUID)
    val script = safePathToFile(scriptData.scriptPath)
    val content = script.content
    val outputStream = StringPrintStream(Some(preference(outputSize)))

    execution.addStaticInfo(execId, StaticExecutionInfo(scriptData.scriptPath, content, System.currentTimeMillis()))
    execution.addOutputStreams(execId, outputStream)

    def error(t: Throwable): Unit = execution.addError(execId, Failed(ErrorBuilder(t), Seq()))
    def message(message: String): Unit = execution.addError(execId, Failed(Error(message), Seq()))

    try {
      val project = Project(script.getParentFileSafe)
      project.compile(script, Seq.empty) match {
        case ScriptFileDoesNotExists() ⇒ message("Script file does not exist")
        case ErrorInCode(e)            ⇒ error(e)
        case ErrorInCompiler(e)        ⇒ error(e)
        case compiled: Compiled ⇒

          def catchAll[T](f: ⇒ T): Try[T] = {
            val res =
              try Success(f)
              catch {
                case t: Throwable ⇒ Failure(t)
              }
            res
          }

          catchAll(OutputManager.withStreamOutputs(outputStream, outputStream)(compiled.eval)) match {
            case Failure(e) ⇒ error(e)
            case Success(o) ⇒
              val puzzle = o.buildPuzzle

              Try(puzzle.toExecution(executionContext = MoleExecutionContext(out = outputStream))) match {
                case Success(ex) ⇒
                  val envIds = (ex.allEnvironments).map { env ⇒ EnvironmentId(getUUID, execId) → env }
                  execution.addRunning(execId, envIds)
                  envIds.foreach { case (envId, env) ⇒ env.listen(execution.environmentListener(envId)) }

                  Try(ex.start) match {
                    case Failure(e) ⇒ error(e)
                    case Success(_) ⇒
                      val inserted = execution.addMoleExecution(execId, ex)
                      if (!inserted) ex.cancel
                  }
                case Failure(e) ⇒ error(e)
              }
          }
      }
    }
    catch {
      case t: Throwable ⇒ error(t)
    }
  }

  def allStates(lines: Int) = execution.allStates(lines)

  def staticInfos() = execution.staticInfos()

  def clearEnvironmentErrors(environmentId: EnvironmentId): Unit = execution.deleteEnvironmentErrors(environmentId)

  def runningErrorEnvironmentData(environmentId: EnvironmentId, lines: Int): EnvironmentErrorData = atomic { implicit ctx ⇒
    val errorMap = execution.getRunningEnvironments(environmentId).toMap
    val info = errorMap(environmentId)

    val environmentErrors =
      info.environment.errors.map {
        ex ⇒ EnvironmentError(environmentId, ex.exception.getMessage, ErrorBuilder(ex.exception), ex.creationTime, Utils.javaLevelToErrorLevel(ex.level))
      }

    def groupedErrors = environmentErrors.groupBy {
      _.errorMessage
    }.toSeq.map {
      case (_, err) ⇒
        val dates = err.map { _.date }.sorted
        (err.head, dates.max, dates.size)
    }.takeRight(lines)

    EnvironmentErrorData(groupedErrors)
  }

  def marketIndex() = {
    def mapToMd(marketIndex: MarketIndex) =
      marketIndex.copy(entries = marketIndex.entries.map {
        e ⇒
          e.copy(readme = e.readme.map {
            MarkDownProcessor(_)
          })
      })

    mapToMd(market.marketIndex)
  }

  def getMarketEntry(entry: MarketIndexEntry, path: SafePath) = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    market.downloadEntry(entry, safePathToFile(path))
    autoAddPlugins(path)
  }

  //PLUGINS
  def addPlugins(nodes: Seq[String]): Seq[Error] = {
    val plugins = nodes.map(Utils.pluginUpdoadDirectory / _)
    val errors = module.addPluginsFiles(plugins, true, Some(module.pluginDirectory))
    plugins.foreach(_.recursiveDelete)
    errors.map(e ⇒ ErrorBuilder(e._2))
  }

  def autoAddPlugins(path: SafePath) = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val file = safePathToFile(path)

    def recurse(f: File): List[File] = {
      val subPlugins: List[File] = if (f.isDirectory) f.listFilesSafe.toList.flatMap(recurse) else Nil
      PluginManager.listBundles(f).toList ::: subPlugins
    }

    module.addPluginsFiles(recurse(file), false)
  }

  def isPlugin(path: SafePath): Boolean = Utils.isPlugin(path)

  def allPluggableIn(path: SafePath): Seq[SafePath] = Utils.allPluggableIn(path)

  def listPlugins(): Iterable[Plugin] =
    module.pluginDirectory.listFilesSafe.map(p ⇒ Plugin(p.getName, new SimpleDateFormat("dd/MM/yyyy HH:mm").format(p.lastModified)))

  def removePlugin(plugin: Plugin): Unit = synchronized {
    val file = module.pluginDirectory / plugin.name
    val allDependingFiles = PluginManager.allDepending(file, b ⇒ !b.isProvided)
    val bundle = PluginManager.bundle(file)
    bundle.foreach(PluginManager.remove)
    allDependingFiles.filter(f ⇒ !PluginManager.bundle(f).isDefined).foreach(_.recursiveDelete)
    file.recursiveDelete
  }

  //GUI OM PLUGINS

  def getGUIPlugins(): AllPluginExtensionData = {

    AllPluginExtensionData(
      authentications = PluginActivator.authentications
    )
  }

  //MODEL WIZARDS
  def launchingCommands(path: SafePath): Seq[LaunchingCommand] = Utils.launchinCommands(path)

  //Extract models from an archive
  def models(archivePath: SafePath): Seq[SafePath] = {
    val toDir = archivePath.toNoExtention
    // extractTGZToAndDeleteArchive(archivePath, toDir)
    (for {
      tnd ← listFiles(toDir).list if FileType.isSupportedLanguage(tnd.name)
    } yield tnd).map { nd ⇒ toDir ++ nd.name }
  }

  def classes(jarPath: SafePath): Seq[ClassTree] = Utils.jarClasses(jarPath)

  def methods(jarPath: SafePath, className: String): Seq[JarMethod] = Utils.jarMethods(jarPath, className)

  def buildModelTask(
    executableName: String,
    scriptName:     String,
    command:        String,
    language:       Language,
    inputs:         Seq[ProtoTypePair],
    outputs:        Seq[ProtoTypePair],
    path:           SafePath,
    imports:        Option[String],
    libraries:      Option[String],
    resources:      Resources
  ): SafePath = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val modelTaskFile = new File(path, scriptName + ".oms")

    val os = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(modelTaskFile)))

    def ioString(protos: Seq[ProtoTypePair], keyString: String) = if (protos.nonEmpty) Seq(s"  $keyString += (", ")").mkString(protos.map { i ⇒ s"${i.name}" }.mkString(", ")) + ",\n" else ""

    def imapString(protos: Seq[ProtoTypePair], keyString: String) = if (protos.nonEmpty) protos.map { i ⇒ s"""  $keyString += (${i.name}, "${i.mapping.get}")""" }.mkString(",\n") + ",\n" else ""

    def omapString(protos: Seq[ProtoTypePair], keyString: String) = if (protos.nonEmpty) protos.map { o ⇒ s"""  $keyString += ("${o.mapping.get}", ${o.name})""" }.mkString(",\n") + ",\n" else ""

    def default(key: String, value: String) = s"  $key := $value"

    try {
      imports.foreach { i ⇒ os.write(s"import $i._\n") }
      for (p ← ((inputs ++ outputs).map { p ⇒ (p.name, p.`type`.scalaString) } distinct)) yield {
        os.write("val " + p._1 + " = Val[" + p._2 + "]\n")
      }

      val (rawimappings, ins) = inputs.partition(i ⇒ i.mapping.isDefined)
      val (rawomappings, ous) = outputs.partition(o ⇒ o.mapping.isDefined)
      val (ifilemappings, imappings) = rawimappings.partition(_.`type` == ProtoTYPE.FILE)
      val (ofilemappings, omappings) = rawomappings.partition(_.`type` == ProtoTYPE.FILE)

      val inString = ioString(ins, "inputs")
      val imFileString = imapString(ifilemappings, "inputFiles")
      val ouString = ioString(ous, "outputs")
      val omFileString = omapString(ofilemappings, "outputFiles")
      val resourcesString = if (resources.all.nonEmpty) s"""  resources += (${resources.all.map { r ⇒ s"workDirectory / ${(r.safePath.path.drop(1).mkString("/")).mkString(",")}" }}).\n""" else ""
      val defaults =
        "  //Default values. Can be removed if OpenMOLE Vals are set by values coming from the workflow\n" +
          (inputs.map { p ⇒ (p.name, testBoolean(p)) } ++
            ifilemappings.map { p ⇒ (p.name, " workDirectory / \"" + p.mapping.getOrElse("") + "\"") }).filterNot {
              _._2.isEmpty
            }.map { p ⇒ default(p._1, p._2) }.mkString(",\n")

      language.taskType match {
        case ctt: CareTaskType ⇒
          os.write(
            s"""\nval task = CARETask(workDirectory / "$executableName", "$command") set(\n""" +
              inString + ouString + imFileString + omFileString + resourcesString + defaults
          )
        case ntt: NetLogoTaskType ⇒
          val imString = imapString(imappings, "netLogoInputs")
          val omString = omapString(omappings, "netLogoOutputs")
          os.write(
            s"""\nval launch = List("${(Seq("setup", "random-seed ${seed}") ++ (command.split('\n').toSeq)).mkString("\",\"")}")
               \nval task = NetLogo5Task(workDirectory / ${executableName.split('/').map { s ⇒ s"""\"$s\"""" }.mkString(" / ")}, launch, embedWorkspace = ${!resources.implicits.isEmpty}) set(\n""".stripMargin +
              inString + ouString + imString + omString + imFileString + omFileString + defaults
          )
        case st: ScalaTaskType ⇒
          os.write(
            s"""\nval task = ScalaTask(\n\"\"\"$command\"\"\") set(\n""" +
              s"${libraries.map { l ⇒ s"""  libraries += workingDirectory / "$l",""" }.getOrElse("")}\n\n" +
              inString + ouString + imFileString + omFileString + resourcesString + defaults
          )

        case _ ⇒ ""
      }
      os.write("\n  )\n\ntask hook ToStringHook()")
    }

    finally {
      os.close
    }
    modelTaskFile.createNewFile
    modelTaskFile
  }

  def expandResources(resources: Resources): Resources = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val paths = safePath(resources.all.map {
      _.safePath
    }).distinct.map { sp ⇒ Resource(sp, safePathToFile(sp).length) }
    val implicitResource = resources.implicits.map { r ⇒
      Resource(safePath(r.safePath), safePathToFile(r.safePath).length)
    }

    Resources(
      paths,
      implicitResource,
      paths.size + implicitResource.size
    )
  }

  def testBoolean(protoType: ProtoTypePair) = protoType.`type` match {
    case ProtoTYPE.BOOLEAN ⇒ if (protoType.default == "1") "true" else "false"
    case _                 ⇒ protoType.default
  }
}
