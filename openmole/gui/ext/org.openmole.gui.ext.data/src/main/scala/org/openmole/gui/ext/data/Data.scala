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

case class DataBag(uuid: String, name: String, data: Data)

trait Data

object ProtoTYPE {
  case class ProtoTYPE(uuid: String, name: String, scalaString: String)

  val INT = new ProtoTYPE("Integer", "Integer", "Int")
  val DOUBLE = new ProtoTYPE("Double", "Double", "Double")
  val LONG = new ProtoTYPE("Long", "Long", "Long")
  val BOOLEAN = new ProtoTYPE("Boolean", "Boolean", "Boolean")
  val STRING = new ProtoTYPE("String", "String", "String")
  val FILE = new ProtoTYPE("File", "File", "File")
  val ALL = Seq(INT, DOUBLE, LONG, BOOLEAN, STRING, FILE)

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

case class ErrorData(data: DataBag, error: String, stack: String)

case class IOMappingData[T](key: String, value: T)

case class InOutput(prototype: PrototypeData, mappings: Seq[IOMappingData[_]])

case class InAndOutput(inputPrototype: PrototypeData, outputPrototype: PrototypeData, mapping: IOMappingData[_])

sealed trait FileExtension {
  def displayable: Boolean
}

case class DisplayableFile(highlighter: String, displayable: Boolean = true) extends FileExtension

case class OpenMOLEScript(highlighter: String, displayable: Boolean = true) extends FileExtension

case class MDScript(displayable: Boolean = true) extends FileExtension

case class DisplayableOnDemandFile(highlighter: String, displayable: Boolean = true) extends FileExtension

case class BinaryFile() extends FileExtension {
  def displayable = false
}

case class TarGz() extends FileExtension {
  def displayable = false
}

object FileExtension {
  val OMS = OpenMOLEScript("scala")
  val SCALA = DisplayableOnDemandFile("scala")
  val MD = MDScript()
  val SH = DisplayableOnDemandFile("sh")
  val TEXT = DisplayableOnDemandFile("text")
  val NO_EXTENSION = DisplayableFile("text")
  val TGZ = TarGz()
  val BINARY = BinaryFile()
}

sealed trait FileContent

case class AlterableFileContent(path: SafePath, content: String) extends FileContent

case class AlterableOnDemandFileContent(path: SafePath, content: String, editable: () ⇒ Boolean) extends FileContent

case class ReadOnlyFileContent() extends FileContent

object SafePath {
  def sp(path: Seq[String], extension: FileExtension = FileExtension.NO_EXTENSION): SafePath =
    SafePath(path, extension)

  def leaf(name: String, extension: FileExtension) = sp(Seq(name), extension)

  def empty = leaf("", FileExtension.NO_EXTENSION)
}

import org.openmole.gui.ext.data.SafePath._

//The path it relative to the project root directory
case class SafePath(path: Seq[String], extension: FileExtension) {
  def /(safePath: SafePath) = sp(this.path ++ safePath.path, safePath.extension)

  def ++(s: String) = sp(this.path :+ s, this.extension)

  def parent: SafePath = SafePath(path.dropRight(1), extension)

  def name = path.last
}

sealed trait AuthenticationData extends Data {
  def synthetic: String
}

trait PrivateKey {
  def privateKey: Option[String]
}

case class LoginPasswordAuthenticationData(login: String = "",
                                           cypheredPassword: String = "",
                                           target: String = "") extends AuthenticationData {
  def synthetic = s"$login@$target"
}

case class PrivateKeyAuthenticationData(privateKey: Option[String] = None,
                                        login: String = "",
                                        cypheredPassword: String = "",
                                        target: String = "") extends AuthenticationData with PrivateKey {
  def synthetic = s"$login@$target"
}

case class EGIP12AuthenticationData(val cypheredPassword: String = "",
                                    val privateKey: Option[String] = None) extends AuthenticationData with PrivateKey {
  def synthetic = "egi.p12"
}

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

@JSExport
case class TreeNodeData(
                         name: String,
                         safePath: SafePath,
                         isDirectory: Boolean,
                         isPlugin: Boolean,
                         size: Long,
                         readableSize: String)

@JSExport
case class ScriptData(scriptPath: SafePath)

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

case class EnvironmentId(id: String = java.util.UUID.randomUUID.toString) extends ID

sealed trait ErrorStateLevel {
  def name: String
}

case class DebugLevel() extends ErrorStateLevel {
  val name = "DEBUG"
}

case class ErrorLevel() extends ErrorStateLevel {
  val name = "ERROR"
}

case class EnvironmentError(environmentId: EnvironmentId, errorMessage: String, stack: Error, date: Long, level: ErrorStateLevel)

case class NetworkActivity(downloadingFiles: Int = 0,
                           downloadedSize: Long = 0L,
                           readableDownloadedSize: String = "",
                           uploadingFiles: Int = 0,
                           uploadedSize: Long = 0L,
                           readableUploadedSize: String = "")

case class RunningEnvironmentData(id: ExecutionId, errors: Seq[(EnvironmentError, Int)])

case class RunningOutputData(id: ExecutionId, output: String)

case class StaticExecutionInfo(path: SafePath, script: String, startDate: Long = 0L)

case class EnvironmentState(envId: EnvironmentId, taskName: String, running: Long, done: Long, submitted: Long, failed: Long, networkActivity: NetworkActivity)

//case class Output(output: String)

sealed trait ExecutionInfo {
  def state: String

  def duration: Long

  def ready: Long

  def running: Long

  def completed: Long
}

case class Failed(error: Error, duration: Long = 0L, completed: Long = 0L) extends ExecutionInfo {
  def state: String = "failed"

  def running = 0L

  def ready: Long = 0L
}

case class Running(ready: Long,
                   running: Long,
                   duration: Long,
                   completed: Long,
                   environmentStates: Seq[EnvironmentState]) extends ExecutionInfo {
  def state: String = "running"
}

case class Finished(duration: Long = 0L,
                    completed: Long = 0L,
                    environmentStates: Seq[EnvironmentState]) extends ExecutionInfo {
  def ready: Long = 0L

  def running: Long = 0L

  def state: String = "finished"
}

case class Canceled(duration: Long = 0L, completed: Long = 0L) extends ExecutionInfo {
  def state: String = "cancelled"

  def running = 0L

  def ready: Long = 0L
}

case class Ready() extends ExecutionInfo {
  def state: String = "ready"

  def duration: Long = 0L

  def completed: Long = 0L

  def ready: Long = 0L

  def running = 0L
}

case class PasswordState(chosen: Boolean, hasBeenSet: Boolean)

case class Plugin(name: String)

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

case class ScalaTaskType() extends TaskType

case class NetLogoTaskType() extends TaskType

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
  val taskType = CareTaskType()
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

sealed trait CommandElement {
  def expand: String
  def index: Int
}
case class StaticElement(index: Int, expand: String) extends CommandElement

case class VariableElement(index: Int, prototype: ProtoTypePair, taskType: TaskType) extends CommandElement{
  def expand = taskType.preVariable + prototype.name + taskType.postVariable
  def clone(newPrototypePair: ProtoTypePair): VariableElement = copy(prototype = newPrototypePair)
  def clone(newName: String, newType: ProtoTYPE, newMapping: Option[String]): VariableElement = clone(prototype.copy(name = newName, `type` = newType, mapping = newMapping))
}

case class LaunchingCommand(language: Option[Language], codeName: String, arguments: Seq[CommandElement] = Seq(), outputs: Seq[VariableElement] = Seq()) {
  def fullCommand: String = language match {
    case Some(NetLogoLanguage())=> "setup\ngo"
    case _=> (Seq(language.map{_.name}.getOrElse(""), codeName) ++ arguments.sortBy{_.index}.map{_.expand}).mkString(" ")
  }
  def statics: Seq[StaticElement] = arguments.collect{case a: StaticElement=> a}
}

case class ProtoTypePair(name: String, `type`: ProtoTYPE.ProtoTYPE, default: String = "", mapping: Option[String] = None)

case class ClassTree(name: String, classes: Seq[String])