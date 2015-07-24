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

import java.net.URI

case class DataBag(uuid: String, name: String, data: Data)

trait Data

object ProtoTYPE extends Enumeration {

  case class ProtoTYPE(uuid: String, name: String) extends Val(name)

  val INT = new ProtoTYPE("Integer", "Integer")
  val DOUBLE = new ProtoTYPE("Double", "Double")
  val LONG = new ProtoTYPE("Long", "Long")
  val BOOLEAN = new ProtoTYPE("Boolean", "Boolean")
  val STRING = new ProtoTYPE("String", "String")
  val FILE = new ProtoTYPE("File", "File")
  val ALL = Seq(INT, DOUBLE, LONG, BOOLEAN, STRING, FILE)
}

import ProtoTYPE._
import java.io.{ StringWriter, PrintWriter }
import scala.scalajs.js.annotation.JSExport
import upickle._

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
  val NETLOGO = DisplayableOnDemandFile("nlogo")
  val MD = MDScript()
  val SH = DisplayableOnDemandFile("sh")
  val CSV = DisplayableOnDemandFile("text")
  val NO_EXTENSION = DisplayableFile("text")
  val TGZ = TarGz()
  val BINARY = BinaryFile()
}

sealed trait FileContent

// def alterability: Alterability

/*
sealed trait Alterability

case class Alterable() extends Alterability

case class ReadOnly() extends Alterability*/

case class AlterableFileContent(path: SafePath, content: String) extends FileContent

case class AlterableOnDemandFileContent(path: SafePath, content: String, editable: () ⇒ Boolean) extends FileContent

case class ReadOnlyFileContent() extends FileContent

object SafePath {
  def sp(path: Seq[String], extension: FileExtension = FileExtension.NO_EXTENSION): SafePath =
    SafePath(path, extension)

  def leaf(name: String, extension: FileExtension) = sp(Seq(name), extension)

  def empty = leaf("", FileExtension.NO_EXTENSION)
}

import SafePath._

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

sealed trait UploadType

case class UploadProject() extends UploadType

case class UploadKey() extends UploadType

@JSExport
case class TreeNodeData(
  name: String,
  safePath: SafePath,
  isDirectory: Boolean,
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

case class ExecutionId(id: String = java.util.UUID.randomUUID.toString)

case class EnvironmentId(id: String = java.util.UUID.randomUUID.toString)

case class EnvironmentError(id: EnvironmentId, errorMessage: String, stack: Error)

case class NetworkActivity(downloadingFiles: Int = 0,
                           downloadedSize: Long = 0L,
                           readableDownloadedSize: String = "",
                           uploadingFiles: Int = 0,
                           uploadedSize: Long = 0L,
                           readableUploadedSize: String = "")

case class RunningEnvironmentData(id: ExecutionId, errors: Seq[EnvironmentError])

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
  def state: String = "canceled"

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
