package org.openmole.gui.ext.data

/*
 * Copyright (C) 25/09/14 // mathieu.leclaire@openmole.org
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

import monocle.macros.Lenses

trait Data

object ProtoTYPE {

  case class ProtoTYPE(uuid: String, name: String, scalaString: String)

  val INT = new ProtoTYPE("Integer", "Integer", "Int")
  val DOUBLE = new ProtoTYPE("Double", "Double", "Double")
  val LONG = new ProtoTYPE("Long", "Long", "Long")
  val BOOLEAN = new ProtoTYPE("Boolean", "Boolean", "Boolean")
  val STRING = new ProtoTYPE("String", "String", "String")
  val FILE = new ProtoTYPE("File", "File", "File")
  val CHAR = new ProtoTYPE("Char", "Char", "Char")
  val SHORT = new ProtoTYPE("Short", "Short", "Short")
  val BYTE = new ProtoTYPE("Byte", "Byte", "Byte")
  val ALL = Seq(INT, DOUBLE, LONG, BOOLEAN, STRING, FILE, CHAR, SHORT, BYTE)

//  implicit def scalaTypeStringToPrototype(s: String): ProtoTYPE =
//    if (s.contains("Double")) DOUBLE
//    else if (s.contains("Int")) INT
//    else if (s.contains("Long")) LONG
//    else if (s.contains("File")) FILE
//    else STRING
}

import java.io.{PrintWriter, StringWriter}

import org.openmole.gui.ext.data.ProtoTYPE._

import scala.scalajs.js.annotation.JSExport

class PrototypeData(val `type`: ProtoTYPE, val dimension: Int) extends Data

class IntPrototypeData(dimension: Int) extends PrototypeData(INT, dimension)

class DoublePrototypeData(dimension: Int) extends PrototypeData(DOUBLE, dimension)

class StringPrototypeData(dimension: Int) extends PrototypeData(STRING, dimension)

class LongPrototypeData(dimension: Int) extends PrototypeData(LONG, dimension)

class BooleanPrototypeData(dimension: Int) extends PrototypeData(BOOLEAN, dimension)

class FilePrototypeData(dimension: Int) extends PrototypeData(FILE, dimension)

object PrototypeData {

  def apply(`type`: ProtoTYPE, dimension: Int) = new PrototypeData(`type`, dimension)

  def integer(dimension: Int) = new IntPrototypeData(dimension)

  def double(dimension: Int) = new DoublePrototypeData(dimension)

  def long(dimension: Int) = new LongPrototypeData(dimension)

  def boolean(dimension: Int) = new BooleanPrototypeData(dimension)

  def string(dimension: Int) = new StringPrototypeData(dimension)

  def file(dimension: Int) = new FilePrototypeData(dimension)

}

trait InputData <: Data {
  def inputs: Seq[InOutput]
}

trait OutputData <: Data {
  def outputs: Seq[InOutput]
}

trait InAndOutputData <: Data {
  def inAndOutputs: Seq[InAndOutput]
}

trait TaskData extends Data with InputData with OutputData

trait EnvironmentData extends Data

trait HookData extends Data with InputData with OutputData

case class IOMappingData[T](key: String, value: T)

case class InOutput(prototype: PrototypeData, mappings: Seq[IOMappingData[_]])

case class InAndOutput(inputPrototype: PrototypeData, outputPrototype: PrototypeData, mapping: IOMappingData[_])

sealed trait FileExtension {
  def displayable: Boolean
}

trait HighlightedFile {
  def highlighter: String
}

object OpenMOLEScript extends FileExtension with HighlightedFile {
  val displayable = true
  val highlighter = "openmole"
}

object MDScript extends FileExtension {
  val displayable = true
}

object SVGExtension extends FileExtension {
  val displayable = true
}

case class EditableFile(highlighter: String, onDemand: Boolean = false) extends FileExtension with HighlightedFile {
  val displayable = true
}

object BinaryFile extends FileExtension {
  val displayable = false
}

object TarGz extends FileExtension {
  def displayable = false
}

object Tar extends FileExtension {
  def displayable = false
}

object Zip extends FileExtension {
  def displayable = false
}

object TgzBin extends FileExtension {
  def displayable = false
}

object Jar extends FileExtension {
  def displayable = false
}

object FileExtension {
  val OMS = OpenMOLEScript
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
  val TAR = Tar
  val ZIP = Zip
  val TGZBIN = TgzBin
  val JAR = Jar
  val BINARY = BinaryFile
}

sealed trait FileContent

case class AlterableFileContent(path: SafePath, content: String) extends FileContent

case class ReadOnlyFileContent() extends FileContent

object SafePath {
  def sp(path: Seq[String]): SafePath =
    SafePath(path)

  def leaf(name: String) = sp(Seq(name))

  def empty = leaf("")

  def naming = (sp: SafePath) ⇒ sp.name

  def allSafePaths(path: Seq[String]): Seq[SafePath] = {
    def all(todo: Seq[String], done: Seq[SafePath]): Seq[SafePath] = {
      if (todo.isEmpty) done
      else all(todo.dropRight(1), done :+ SafePath.sp(todo))
    }

    all(path, Seq())
  }
}

import org.openmole.gui.ext.data.SafePath._

sealed trait ServerFileSystemContext

case class AbsoluteFileSystem() extends ServerFileSystemContext

case class ProjectFileSystem() extends ServerFileSystemContext

object ServerFileSystemContext {
  implicit val absolute: ServerFileSystemContext = AbsoluteFileSystem()
  implicit val project: ServerFileSystemContext = ProjectFileSystem()
}

//The path it relative to the project root directory
case class SafePath(path: Seq[String], context: ServerFileSystemContext = ProjectFileSystem()) {

  def ++(s: String) = sp(this.path :+ s)

  def parent: SafePath = SafePath.sp(path.dropRight(1))

  def name = path.lastOption.getOrElse("")

  def isEmpty = path.isEmpty

  def toNoExtention = copy(path = path.dropRight(1) :+ nameWithNoExtension)

  def nameWithNoExtension = name.split('.').head

  def normalizedPathString = path.tail.mkString("/")
}

object ExtractResult {
  def ok = ExtractResult(None)
}

case class ExtractResult(error: Option[Error])

sealed trait UploadType {
  def typeName: String
}

case class UploadProject() extends UploadType {
  def typeName = "project"
}

case class UploadAuthentication() extends UploadType {
  def typeName = "authentication"
}

case class UploadPlugin() extends UploadType {
  def typeName = "plugin"
}

case class UploadAbsolute() extends UploadType {
  def typeName = "absolute"
}

case class DirData(isEmpty: Boolean)

case class TreeNodeData(
                         name: String,
                         dirData: Option[DirData],
                         size: Long,
                         time: Long
                       )

case class ScriptData(scriptPath: SafePath)

object Error {
  def empty = Error("")
}

case class Error(stackTrace: String)

case class Token(token: String, duration: Long)

object ErrorBuilder {
  def apply(t: Throwable): Error = {
    val sw = new StringWriter()
    t.printStackTrace(new PrintWriter(sw))
    Error(sw.toString)
  }

  def apply(stackTrace: String) = Error(stackTrace)
}

sealed trait ID {
  def id: String
}

case class ExecutionId(id: String = java.util.UUID.randomUUID.toString) extends ID

case class EnvironmentId(id: String = java.util.UUID.randomUUID.toString, executionId: ExecutionId) extends ID

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
                             stack: Error,
                             date: Long,
                             level: ErrorStateLevel
                           ) extends Ordered[EnvironmentError] {
  def compare(that: EnvironmentError) = date compare that.date
}

@Lenses case class NetworkActivity(
                                    downloadingFiles: Int = 0,
                                    downloadedSize: Long = 0L,
                                    readableDownloadedSize: String = "",
                                    uploadingFiles: Int = 0,
                                    uploadedSize: Long = 0L,
                                    readableUploadedSize: String = ""
                                  )

@Lenses case class ExecutionActivity(executionTime: Long = 0)

object EnvironmentErrorData {
  def empty = EnvironmentErrorData(Seq())
}

// datedError is a triplet of (EnvironmentError, most recent occurrence, number of occurrences)
case class EnvironmentErrorData(datedErrors: Seq[(EnvironmentError, Long, Int)])

case class OutputStreamData(id: ExecutionId, output: String)

case class StaticExecutionInfo(path: SafePath, script: String, startDate: Long = 0L)

case class EnvironmentState(
                             envId: EnvironmentId,
                             taskName: String,
                             running: Long,
                             done: Long,
                             submitted: Long,
                             failed: Long,
                             networkActivity: NetworkActivity,
                             executionActivity: ExecutionActivity
                           )

//case class Output(output: String)

sealed trait ExecutionInfo {
  def state: String

  def duration: Long

  def capsules: Vector[(ExecutionInfo.CapsuleId, ExecutionInfo.JobStatuses)]

  def ready: Long = capsules.map(_._2.ready).sum

  def running: Long = capsules.map(_._2.running).sum

  def completed: Long = capsules.map(_._2.completed).sum

  def environmentStates: Seq[EnvironmentState]
}

object ExecutionInfo {

  type CapsuleId = String

  case class JobStatuses(ready: Long, running: Long, completed: Long)

  case class Failed(
                     capsules: Vector[(ExecutionInfo.CapsuleId, ExecutionInfo.JobStatuses)],
                     error: Error,
                     environmentStates: Seq[EnvironmentState],
                     duration: Long = 0L) extends ExecutionInfo {
    def state: String = "failed"
  }

  case class Running(
                      capsules: Vector[(ExecutionInfo.CapsuleId, ExecutionInfo.JobStatuses)],
                      duration: Long,
                      environmentStates: Seq[EnvironmentState]) extends ExecutionInfo {
    def state: String = "running"
  }

  case class Finished(
                       capsules: Vector[(ExecutionInfo.CapsuleId, ExecutionInfo.JobStatuses)],
                       duration: Long = 0L,
                       environmentStates: Seq[EnvironmentState]) extends ExecutionInfo {
    def state: String = "finished"
  }

  case class Canceled(
                       capsules: Vector[(ExecutionInfo.CapsuleId, ExecutionInfo.JobStatuses)],
                       environmentStates: Seq[EnvironmentState],
                       duration: Long = 0L) extends ExecutionInfo {
    def state: String = "canceled"
  }

  case class Launching() extends ExecutionInfo {
    def state: String = "launching"

    def duration: Long = 0L

    def capsules = Vector.empty

    def environmentStates: Seq[EnvironmentState] = Seq()
  }

}

case class PasswordState(chosen: Boolean, hasBeenSet: Boolean)

case class Plugin(name: String, time: String)

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
  implicit def safePathToFileType(sp: SafePath): FileType = apply(sp)

  implicit def filNameToFileType(f: String): FileType = apply(f)

  private def extension(safePath: SafePath) = safePath.name.split('.').drop(1).mkString(".")

  def apply(safePath: SafePath): FileType = apply(safePath.name)

  def apply(fileName: String): FileType = {
    if (fileName.endsWith("tar.gz.bin") || fileName.endsWith("tgz.bin")) CareArchive
    else if (fileName.endsWith("nlogo")) CodeFile(NetLogoLanguage())
    else if (fileName.endsWith("R")) CodeFile(RLanguage())
    else if (fileName.endsWith("jar")) JarArchive
    else if (fileName.endsWith("tgz") || fileName.endsWith("tar.gz")) Archive
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

case class VariableElement(index: Int, prototype: ProtoTypePair, taskType: TaskType) extends CommandElement {
  def expand =
    if (prototype.`type` == FILE) prototype.mapping.getOrElse("")
    else taskType.preVariable + prototype.name + taskType.postVariable

  def clone(newPrototypePair: ProtoTypePair): VariableElement = copy(prototype = newPrototypePair)

  def clone(newName: String, newType: ProtoTYPE, newMapping: Option[String]): VariableElement = clone(prototype.copy(name = newName, `type` = newType, mapping = newMapping))
}

sealed trait LaunchingCommand {
  def language: Option[Language]

  def arguments: Seq[CommandElement]

  def outputs: Seq[VariableElement]

  def fullCommand: String

  def statics: Seq[StaticElement] = arguments.collect { case a: StaticElement ⇒ a }

  def updateVariables(variableArgs: Seq[VariableElement]): LaunchingCommand
}

case class BasicLaunchingCommand(language: Option[Language], codeName: String, arguments: Seq[CommandElement] = Seq(), outputs: Seq[VariableElement] = Seq()) extends LaunchingCommand {
  def fullCommand: String = language match {
    case Some(NetLogoLanguage()) ⇒ "go;;You should set your stopping criteria here instead"
    case _ ⇒ (Seq(language.map {
      _.name
    }.getOrElse(""), codeName) ++ arguments.sortBy {
      _.index
    }.map {
      _.expand
    }).mkString(" ")
  }

  def updateVariables(variableArgs: Seq[VariableElement]) = copy(arguments = statics ++ variableArgs)
}

case class JavaLaunchingCommand(jarMethod: JarMethod, arguments: Seq[CommandElement] = Seq(), outputs: Seq[VariableElement] = Seq()) extends LaunchingCommand {

  val language = Some(JavaLikeLanguage())

  def fullCommand: String = {
    if (jarMethod.methodName.isEmpty) ""
    else {
      if (jarMethod.isStatic) jarMethod.clazz + "." else s"val constr = new ${jarMethod.clazz}() // You should initialize this constructor first\nconstr."
    } +
      jarMethod.methodName + "(" + arguments.sortBy {
      _.index
    }.map {
      _.expand
    }.mkString(", ") + ")"
  }

  def updateVariables(variableArgs: Seq[VariableElement]) = copy(arguments = statics ++ variableArgs)
}

case class ProtoTypePair(name: String, `type`: ProtoTYPE.ProtoTYPE, default: String = "", mapping: Option[String] = None)

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
                       override val display: String = "Finalizing..."
                     ) extends ProcessState

case class Processed(override val ratio: Int = 100) extends ProcessState

case class JarMethod(methodName: String, argumentTypes: Seq[String], returnType: String, isStatic: Boolean, clazz: String) {
  val expand = methodName + "(" + argumentTypes.mkString(",") + "): " + returnType
}

//case class JarMethod(methodName: String, arguments: Seq[ProtoTypePair], returnType: String, isStatic: Boolean, clazz: String) {
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

sealed trait FirstLast

case class First() extends FirstLast

case class Last() extends FirstLast

sealed trait ListOrdering

case class Ascending() extends ListOrdering

case class Descending() extends ListOrdering

sealed trait ListSorting

case class AlphaSorting() extends ListSorting

case class SizeSorting() extends ListSorting

case class TimeSorting() extends ListSorting

case class LevelSorting() extends ListSorting

case class ListSortingAndOrdering(fileSorting: ListSorting = AlphaSorting(), fileOrdering: ListOrdering = Ascending())

object ListSortingAndOrdering {
  def defaultSorting = ListSortingAndOrdering(AlphaSorting(), Ascending())
}

object FileSizeOrdering extends Ordering[TreeNodeData] {
  def compare(tnd1: TreeNodeData, tnd2: TreeNodeData) = tnd1.size compare tnd2.size
}

object AlphaOrdering extends Ordering[TreeNodeData] {
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
}

object TimeOrdering extends Ordering[TreeNodeData] {
  def compare(tnd1: TreeNodeData, tnd2: TreeNodeData) = tnd1.time compare tnd2.time
}

object ListSorting {

  implicit def sortingToOrdering(fs: ListSorting): Ordering[TreeNodeData] =
    fs match {
      case AlphaSorting() ⇒ AlphaOrdering
      case SizeSorting() ⇒ FileSizeOrdering
      case _ ⇒ TimeOrdering
    }
}

case class FileFilter(firstLast: FirstLast = First(), threshold: Option[Int] = Some(20), nameFilter: String = "", fileSorting: ListSorting = AlphaSorting()) {

  def switchTo(newFileSorting: ListSorting) = {
    val fl = {
      if (fileSorting == newFileSorting) {
        firstLast match {
          case First() ⇒ Last()
          case _ ⇒ First()
        }
      }
      else First()
    }
    copy(fileSorting = newFileSorting, firstLast = fl)
  }
}

case class ListFilesData(list: Seq[TreeNodeData], nbFilesOnServer: Int)

object FileFilter {
  def defaultFilter = FileFilter.this (First(), Some(100), "", AlphaSorting())
}

case class OMSettings(workspace: SafePath, version: String, versionName: String, buildTime: String)

sealed trait PluginExtensionType

object AuthenticationExtension extends PluginExtensionType

//TODO: add other extension points
case class AllPluginExtensionData(authentications: Seq[GUIPluginAsJS], wizards: Seq[GUIPluginAsJS])

case class GUIPluginAsJS(jsObject: String)

trait AuthenticationData {
  def name: String
}

trait WizardData

sealed trait Test {
  def passed: Boolean

  def message: String

  def errorStack: Error
}

case class PendingTest() extends Test {
  def passed = false

  def message = "pending"

  def errorStack = Error.empty
}

case class FailedTest(message: String, errorStack: Error) extends Test {
  def passed = false
}

case class PassedTest(message: String) extends Test {
  def passed = true

  override def errorStack = Error.empty
}

object Test {
  def passed(message: String = "OK") = PassedTest(message)

  def pending = PendingTest()

  def error(msg: String, err: Error) = FailedTest(msg, err)
}

case class JVMInfos(javaVersion: String, jvmImplementation: String, processorAvailable: Int, allocatedMemory: Long, totalMemory: Long)

case class SequenceData(header: Seq[String], content: Seq[Array[String]])

case class WizardModelData(
                            vals: String,
                            inputs: String,
                            outputs: String,
                            inputFileMapping: String,
                            outputFileMapping: String,
                            defaults: String,
                            resources: String,
                            specificInputMapping: Option[String] = None,
                            specificOutputMapping: Option[String] = None,
                          )

case class WizardToTask(safePath: SafePath, errors: Seq[Error] = Seq())