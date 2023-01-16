package org.openmole.gui.shared

/*
 * Copyright (C) 10/03/17 // mathieu.leclaire@openmole.org
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

import endpoints4s.algebra

package data {
  val connectionRoute = "connection"
  val shutdownRoute = "application/shutdown"
  val restartRoute = "application/restart"

  val appRoute = "app"

  val downloadFileRoute = "downloadFile"
  val uploadFilesRoute = "uploadFiles"
  val resetPasswordRoute = "resetPassword"

  def downloadFile(uri: String, hash: Boolean = false) = s"/${downloadFileRoute}?path=$uri&hash=$hash"
  def hashHeader = "Content-Hash"


  enum PrototypeData(val name: String, val scalaString: String):
    case Int extends PrototypeData("Integer", "Int")
    case Double extends PrototypeData("Double", "Double")
    case Long extends PrototypeData("Long", "Long")
    case Boolean extends PrototypeData("Boolean", "Boolean")
    case String extends PrototypeData("String", "String")
    case File extends PrototypeData("File", "File")
    case Char extends PrototypeData("Char", "Char")
    case Short extends PrototypeData("Short", "Short")
    case Byte extends PrototypeData("Byte", "Byte")
    case Any(override val name: String, override val scalaString: String) extends PrototypeData(name, scalaString)

  import java.io.{PrintWriter, StringWriter}
  import scala.scalajs.js.annotation.JSExport

  enum FileExtension:
    def displayable =
      this match
        case OpenMOLEScript | OpenMOLEResult | MDScript | SVGExtension => true
        case _: EditableFile => true
        case _ => false

    case OpenMOLEScript, OpenMOLEResult, MDScript, SVGExtension, BinaryFile, TarGz, TarXz, Tar, Zip, Jar
    case EditableFile(highlighter: String, onDemand: Boolean = false) extends FileExtension

  object FileExtension:
    val OMS = OpenMOLEScript
    val OMR = OpenMOLEResult
    val SCALA = EditableFile("scala")
    val NETLOGO = EditableFile("text")
    val R = EditableFile("R")
    val NLS = EditableFile("text")
    val MD = MDScript
    val SH = EditableFile("sh")
    val TEXT = EditableFile("text", true)
    val CSV = EditableFile("text", true)
    val NO_EXTENSION = EditableFile("text")
    val SVG = SVGExtension
    val TGZ = TarGz
    val TXZ = TarXz
    val TAR = Tar
    val ZIP = Zip
    val JAR = Jar
    val BINARY = BinaryFile

    def apply(fileName: String): FileExtension = {
      fileName match {
        case x if x.endsWith(".oms") ⇒ OMS
        case x if x.endsWith(".omr") ⇒ OMR
        case x if x.endsWith(".csv") ⇒ CSV
        case x if x.endsWith(".nlogo") | x.endsWith(".nlogo3d") ⇒ NETLOGO
        case x if x.endsWith(".R") ⇒ R
        case x if x.endsWith(".gaml") |
          x.endsWith(".py") |
          x.endsWith(".txt") | x.endsWith(".nls") ⇒ TEXT
        case x if x.endsWith(".md") ⇒ MD
        case x if x.endsWith(".tgz") | x.endsWith(".tar.gz") ⇒ TGZ
        case x if x.endsWith(".tar.xz") ⇒ TXZ
        case x if x.endsWith(".tar") ⇒ TAR
        case x if x.endsWith(".zip") ⇒ ZIP
        case x if x.endsWith(".jar") ⇒ JAR
        case x if x.endsWith(".scala") ⇒ SCALA
        case x if x.endsWith(".sh") ⇒ SH
        case x if x.endsWith(".svg") ⇒ SVG
        case _ ⇒ BINARY
      }
    }

    def isOMS(fileName: String) = apply(fileName) == OMS


  enum FileContent:
    case AlterableFileContent(path: SafePath, content: String, hash: String) extends FileContent
    case ReadOnlyFileContent extends FileContent

  import org.openmole.gui.shared.data.SafePath._

  object ServerFileSystemContext:
    implicit val absolute: ServerFileSystemContext = ServerFileSystemContext.Absolute
    implicit val project: ServerFileSystemContext = ServerFileSystemContext.Project
    implicit val authentication: ServerFileSystemContext = ServerFileSystemContext.Authentication

  enum ServerFileSystemContext:
    val typeName = this.productPrefix.toLowerCase
    case Absolute, Project, Authentication

  object SafePath:
    def leaf(name: String) = SafePath(Seq(name))

    def empty = leaf("")

    def naming = (sp: SafePath) ⇒ sp.name

  case class SafePath(path: Seq[String], context: ServerFileSystemContext = ServerFileSystemContext.Project):
    def ++(s: String) = copy(path = this.path :+ s)

    def /(child: String) = copy(path = path :+ child)

    def parent: SafePath = copy(path = path.dropRight(1))

    def name = path.lastOption.getOrElse("")

    def isEmpty = path.isEmpty

    def toNoExtention = copy(path = path.dropRight(1) :+ nameWithNoExtension)

    def nameWithNoExtension = name.split('.').head

    def normalizedPathString = path.tail.mkString("/")

    def extension = FileExtension(name)

  case class DirData(isEmpty: Boolean)

  case class PluginState(isPlugin: Boolean, isPlugged: Boolean)

  case class TreeNodeData(
                           name: String,
                           dirData: Option[DirData],
                           size: Long,
                           time: Long,
                           pluginState: PluginState)

  case class ScriptData(scriptPath: SafePath)

  object ErrorData {
    def empty = MessageErrorData("", None)

    def toStackTrace(t: Throwable) = {
      val sw = new StringWriter()
      t.printStackTrace(new PrintWriter(sw))
      sw.toString
    }

    def apply(errors: Seq[ErrorWithLocation], t: Throwable) = CompilationErrorData(errors, toStackTrace(t))

    def apply(t: Throwable): MessageErrorData = MessageErrorData(t.getMessage, Some(toStackTrace(t)))

    def apply(message: String) = MessageErrorData(message, None)

    def stackTrace(e: ErrorData) =
      e match {
        case MessageErrorData(msg, stackTrace) => msg + stackTrace.map("\n" + _).getOrElse("")
        case CompilationErrorData(_, stackTrace) => stackTrace
      }
  }

  sealed trait ErrorData

  case class MessageErrorData(message: String, stackTrace: Option[String]) extends ErrorData

  case class CompilationErrorData(errors: Seq[ErrorWithLocation], stackTrace: String) extends ErrorData


  case class Token(token: String, duration: Long)

  sealed trait ID {
    def id: String
  }

  case class ExecutionId(id: String = DataUtils.uuID) extends ID

  case class EnvironmentId(id: String = DataUtils.uuID) extends ID

  sealed trait ErrorStateLevel {
    def name: String
  }

  case class DebugLevel() extends ErrorStateLevel {
    val name = "DEBUG"
  }

  case class ErrorLevel() extends ErrorStateLevel {
    val name = "ERROR"
  }

  case class EnvironmentError(
                               environmentId: EnvironmentId,
                               errorMessage: String,
                               stack: ErrorData,
                               date: Long,
                               level: ErrorStateLevel) extends Ordered[EnvironmentError] {
    def compare(that: EnvironmentError) = date compare that.date
  }

  case class NetworkActivity(
                              downloadingFiles: Int = 0,
                              downloadedSize: Long = 0L,
                              readableDownloadedSize: String = "",
                              uploadingFiles: Int = 0,
                              uploadedSize: Long = 0L,
                              readableUploadedSize: String = "")

  case class ExecutionActivity(executionTime: Long = 0)

  object EnvironmentErrorData {
    def empty = EnvironmentErrorData(Seq())
  }

  // datedError is a triplet of (EnvironmentError, most recent occurrence, number of occurrences)
  case class EnvironmentErrorData(datedErrors: Seq[(EnvironmentError, Long, Int)])

  case class OutputStreamData(id: ExecutionId, output: String)

  case class StaticExecutionInfo(path: SafePath, script: String, startDate: Long)

  case class EnvironmentState(
                               envId: EnvironmentId,
                               taskName: String,
                               running: Long,
                               done: Long,
                               submitted: Long,
                               failed: Long,
                               networkActivity: NetworkActivity,
                               executionActivity: ExecutionActivity,
                               numberOfErrors: Int)

  sealed trait ExecutionInfo {
    def state: String

    def duration: Long

    def capsules: Seq[ExecutionInfo.CapsuleExecution]

    def ready: Long = capsules.map(_.statuses.ready).sum

    def running: Long = capsules.map(_.statuses.running).sum

    def completed: Long = capsules.map(_.statuses.completed).sum

    def environmentStates: Seq[EnvironmentState]
  }

  object ExecutionInfo {

    case class CapsuleExecution(name: String, scope: String, statuses: ExecutionInfo.JobStatuses)

    case class JobStatuses(ready: Long, running: Long, completed: Long)

    case class Failed(
                       capsules: Seq[ExecutionInfo.CapsuleExecution],
                       error: ErrorData,
                       environmentStates: Seq[EnvironmentState],
                       duration: Long = 0L,
                       clean: Boolean = true) extends ExecutionInfo {
      def state: String = "failed"
    }

    case class Running(
                        capsules: Vector[ExecutionInfo.CapsuleExecution],
                        duration: Long,
                        environmentStates: Seq[EnvironmentState]) extends ExecutionInfo {
      def state: String = "running"
    }

    case class Finished(
                         capsules: Vector[ExecutionInfo.CapsuleExecution],
                         duration: Long = 0L,
                         environmentStates: Seq[EnvironmentState],
                         clean: Boolean) extends ExecutionInfo {
      def state: String = "finished"
    }


    case class Canceled(
                         capsules: Vector[ExecutionInfo.CapsuleExecution],
                         environmentStates: Seq[EnvironmentState],
                         duration: Long = 0L,
                         clean: Boolean) extends ExecutionInfo {
      def state: String = "canceled"
    }

    case class Compiling() extends ExecutionInfo {
      def state: String = "compiling"

      def duration: Long = 0L

      def capsules = Vector.empty

      def environmentStates: Seq[EnvironmentState] = Seq()
    }

    case class Preparing() extends ExecutionInfo {
      def state: String = "preparing"

      def duration: Long = 0L

      def capsules = Vector.empty

      def environmentStates: Seq[EnvironmentState] = Seq()
    }

  }

  case class PasswordState(chosen: Boolean, hasBeenSet: Boolean)

  // projectSafePath is the plugin path in the project tree.
  // The plugin is copied in the plugin directory with the same name.
  case class Plugin(projectSafePath: SafePath, time: String, plugged: Boolean)

  sealed trait Language {
    def name: String

    def extension: String

    def taskType: TaskType
  }

  sealed trait TaskType {
    def preVariable: String = ""

    def postVariable: String = ""
  }

  case class CareTaskType() extends TaskType {
    override val preVariable = """${"""
    override val postVariable = "}"
  }

  case class NoneOSGITaskType() extends TaskType

  case class OSGIJarTaskType() extends TaskType

  case class ScalaTaskType() extends TaskType

  case class NetLogoTaskType() extends TaskType

  case class RTaskType() extends TaskType

  case class UndefinedTaskType() extends TaskType

  sealed trait FileType

  case class CodeFile(language: Language) extends FileType

  object CareArchive extends FileType

  object JarArchive extends FileType

  object Archive extends FileType

  object UndefinedFileType extends FileType

  object FileType {
    private def extension(safePath: SafePath) = safePath.name.split('.').drop(1).mkString(".")

    def apply(safePath: SafePath): FileType = apply(safePath.name)

    def apply(fileName: String): FileType = {
      if (fileName.endsWith("tar.gz.bin") || fileName.endsWith("tgz.bin")) CareArchive
      else if (fileName.endsWith("nlogo")) CodeFile(NetLogoLanguage())
      else if (fileName.endsWith("R")) CodeFile(RLanguage())
      else if (fileName.endsWith("jar")) JarArchive
      else if (fileName.endsWith("tgz") || fileName.endsWith("tar.gz") || fileName.endsWith("tar.xz")) Archive
      else UndefinedFileType
    }

    def isSupportedLanguage(fileName: String): Boolean = {
      apply(fileName) match {
        case CodeFile(_) | CareArchive | JarArchive ⇒ true
        case _ ⇒ false
      }
    }
  }

  case class Binary() extends Language {
    val name: String = "Binary"
    val extension = ""
    val taskType = CareTaskType()
  }

  case class PythonLanguage() extends Language {
    val name: String = "python"
    val extension = "py"
    val taskType = CareTaskType()
  }

  case class RLanguage() extends Language {
    val name: String = "R"
    val extension = "R"
    val taskType = RTaskType()
  }

  case class NetLogoLanguage() extends Language {
    val name: String = "NetLogo"
    val extension = "nlogo"
    val taskType = NetLogoTaskType()
  }

  case class JavaLikeLanguage() extends Language {
    val name: String = "Java/Scala"
    val extension = "jar"
    val taskType = ScalaTaskType()
  }

  case class UndefinedLanguage() extends Language {
    val name = ""
    val extension = ""
    val taskType = UndefinedTaskType()
  }

  sealed trait CommandElement {
    def expand: String

    def index: Int
  }

  case class StaticElement(index: Int, expand: String) extends CommandElement

  case class VariableElement(index: Int, prototype: PrototypePair, taskType: TaskType) extends CommandElement {
    def expand =
      if (prototype.`type` == PrototypeData.File) prototype.mapping.getOrElse("")
      else taskType.preVariable + prototype.name + taskType.postVariable

    def clone(newPrototypePair: PrototypePair): VariableElement = copy(prototype = newPrototypePair)

    def clone(newName: String, newType: PrototypeData, newMapping: Option[String]): VariableElement = clone(prototype.copy(name = newName, `type` = newType, mapping = newMapping))
  }

  case class ModelMetadata(language: Option[Language], inputs: Seq[PrototypePair], outputs: Seq[PrototypePair], command: Option[String], executableName: Option[String], sourcesDirectory: SafePath)

  //  sealed trait LaunchingCommand {
  //    def language: Option[Language]
  //
  //    def arguments: Seq[CommandElement]
  //
  //    def outputs: Seq[VariableElement]
  //
  //    def fullCommand: String
  //
  //    def statics: Seq[StaticElement] = arguments.collect { case a: StaticElement ⇒ a }
  //
  //    def updateVariables(variableArgs: Seq[VariableElement]): LaunchingCommand
  //  }
  //
  //  case class BasicLaunchingCommand(language: Option[Language], codeName: String, arguments: Seq[CommandElement] = Seq(), outputs: Seq[VariableElement] = Seq()) extends LaunchingCommand {
  //    def fullCommand: String = language match {
  //      case Some(NetLogoLanguage()) ⇒ "go ;; You should set your running/stopping criteria here instead"
  //      case _ ⇒ (Seq(language.map {
  //        _.name
  //      }.getOrElse(""), codeName) ++ arguments.sortBy {
  //        _.index
  //      }.map {
  //        _.expand
  //      }).mkString(" ")
  //    }
  //
  //    def updateVariables(variableArgs: Seq[VariableElement]) = copy(arguments = statics ++ variableArgs)
  //  }
  //
  //  case class JavaLaunchingCommand(jarMethod: JarMethod, arguments: Seq[CommandElement] = Seq(), outputs: Seq[VariableElement] = Seq()) extends LaunchingCommand {
  //
  //    val language = Some(JavaLikeLanguage())
  //
  //    def fullCommand: String = {
  //      if (jarMethod.methodName.isEmpty) ""
  //      else {
  //        if (jarMethod.isStatic) jarMethod.clazz + "." else s"val constr = new ${jarMethod.clazz}() // You should initialize this constructor first\nconstr."
  //      } +
  //        jarMethod.methodName + "(" + arguments.sortBy {
  //        _.index
  //      }.map {
  //        _.expand
  //      }.mkString(", ") + ")"
  //    }
  //
  //    def updateVariables(variableArgs: Seq[VariableElement]) = copy(arguments = statics ++ variableArgs)
  //  }

  case class PrototypePair(name: String, `type`: org.openmole.gui.shared.data.PrototypeData, default: String = "", mapping: Option[String] = None)

  sealed trait ClassTree {
    def name: String

    def flatten(prefix: Seq[String]): Seq[FullClass]

    def flatten: Seq[FullClass]
  }

  case class ClassNode(name: String, childs: Seq[ClassTree]) extends ClassTree {
    def flatten(prefix: Seq[String]) = childs.flatMap { c ⇒ c.flatten(prefix :+ name) }

    def flatten = flatten(Seq())
  }

  case class ClassLeaf(name: String) extends ClassTree {
    def flatten = Seq(FullClass(name))

    def flatten(prefix: Seq[String]) = Seq(FullClass((prefix :+ name).mkString(".")))
  }

  case class FullClass(name: String)

  //Processes
  sealed trait ProcessState {
    def ratio: Int = 0

    def display: String = ""
  }

  case class Processing(override val ratio: Int = 0) extends ProcessState {
    override def display: String = "Transferring... " + ratio + " %"
  }

  case class Finalizing(
                         override val ratio: Int = 100,
                         override val display: String = "Finalizing...") extends ProcessState

  case class Processed(override val ratio: Int = 100) extends ProcessState

  case class JarMethod(methodName: String, argumentTypes: Seq[String], returnType: String, isStatic: Boolean, clazz: String) {
    val expand = methodName + "(" + argumentTypes.mkString(",") + "): " + returnType
  }

  //case class JarMethod(methodName: String, arguments: Seq[PrototypePair], returnType: String, isStatic: Boolean, clazz: String) {
  //  val expand = methodName + "(" + arguments.map {
  //    _.`type`.scalaString
  //  }.mkString(",") + "): " + returnType
  //}


  object Resources {
    def empty = Resources(Seq(), Seq(), 0)
  }

  case class Resource(safePath: SafePath, size: Long)

  case class Resources(all: Seq[Resource], implicits: Seq[Resource], number: Int) {
    def withNoImplicit = copy(implicits = Seq())

    def size = all.size + implicits.size
  }

  enum FirstLast:
    case First, Last

  enum ListOrdering:
    case Ascending, Descending

  object ListSorting:
    implicit def sortingToOrdering(fs: ListSorting): Ordering[TreeNodeData] =
      fs match {
        case AlphaSorting ⇒ alphaOrdering
        case SizeSorting ⇒ fileSizeOrdering
        case TimeSorting ⇒ timeOrdering
      }

    def fileSizeOrdering: Ordering[TreeNodeData] = (tnd1, tnd2) => tnd1.size compare tnd2.size

    def alphaOrdering = new Ordering[TreeNodeData]:
      def isDirectory(tnd: TreeNodeData) = tnd.dirData match {
        case None ⇒ false
        case _ ⇒ true
      }

      def compare(tn1: TreeNodeData, tn2: TreeNodeData) =
        if (isDirectory(tn1)) {
          if (isDirectory(tn2)) tn1.name compare tn2.name
          else -1
        }
        else {
          if (isDirectory(tn2)) 1
          else tn1.name compare tn2.name
        }

    def timeOrdering: Ordering[TreeNodeData] = (tnd1, tnd2) => tnd1.time compare tnd2.time


  enum ListSorting:
    case AlphaSorting, SizeSorting, TimeSorting //, LevelSorting
  //case ListSortingAndOrdering(fileSorting: ListSorting = AlphaSorting, fileOrdering: ListOrdering = ListOrdering.Ascending)

  //  object ListSortingAndOrdering {
  //    def defaultSorting = ListSorting.ListSortingAndOrdering(AlphaSorting(), Ascending())
  //  }


  case class FileFilter(firstLast: FirstLast = FirstLast.First, fileSorting: ListSorting = ListSorting.AlphaSorting):

    def switchTo(newFileSorting: ListSorting) =
      val fl =
        if (fileSorting == newFileSorting)
        then
          firstLast match
            case FirstLast.First ⇒ FirstLast.Last
            case _ ⇒ FirstLast.First
        else FirstLast.First
      copy(fileSorting = newFileSorting, firstLast = fl)


  case class ListFilesData(list: Seq[TreeNodeData], nbFilesOnServer: Int)

  object FileFilter {
    def defaultFilter = FileFilter()
  }

  case class OMSettings(workspace: SafePath, version: String, versionName: String, buildTime: String, isDevelopment: Boolean)

  sealed trait PluginExtensionType

  object AuthenticationExtension extends PluginExtensionType

  //TODO: add other extension points
  case class PluginExtensionData(authentications: Seq[GUIPluginAsJS], wizards: Seq[GUIPluginAsJS])

  type GUIPluginAsJS = String

  trait AuthenticationData {
    def name: String
  }

  trait WizardData

  sealed trait Test {
    def passed: Boolean

    def message: String

    def error: Option[ErrorData]
  }

  case class PendingTest() extends Test {
    def passed = false

    def message = "pending"

    def error = None
  }

  case class FailedTest(message: String, errorValue: ErrorData) extends Test {
    def error = Some(errorValue)

    def passed = false
  }

  case class PassedTest(message: String) extends Test {
    def passed = true

    def error = None
  }

  object Test {
    def passed(message: String = "OK") = PassedTest(message)

    def pending = PendingTest()

    def error(msg: String, err: ErrorData) = FailedTest(msg, err)
  }

  case class JVMInfos(javaVersion: String, jvmImplementation: String, processorAvailable: Int, allocatedMemory: Long, totalMemory: Long)

  type SequenceHeader = Seq[String]

  case class SequenceData(header: SequenceHeader = Seq(), content: Seq[Seq[String]] = Seq()) {

    def withRowIndexes = {
      val lineIndexes = (1 to content.length).map {
        _.toString
      }
      copy(header = header :+ "Row index", content = content.zip(lineIndexes).map { case (l, i) ⇒ l :+ i })
    }
  }

  case class WizardModelData(
                              vals: String,
                              inputs: String,
                              outputs: String,
                              inputFileMapping: String,
                              outputFileMapping: String,
                              defaults: String,
                              //   resources: String,
                              specificInputMapping: Option[String] = None,
                              specificOutputMapping: Option[String] = None)

  case class WizardToTask(safePath: SafePath, errors: Seq[ErrorData] = Seq())


  case class ErrorWithLocation(stackTrace: String = "", line: Option[Int] = None, start: Option[Int] = None, end: Option[Int] = None)

  case class ErrorFromCompiler(errorWithLocation: ErrorWithLocation = ErrorWithLocation(), lineContent: String = "")

  case class EditorErrors(errorsFromCompiler: Seq[ErrorFromCompiler] = Seq(), errorsInEditor: Seq[Int] = Seq())

}