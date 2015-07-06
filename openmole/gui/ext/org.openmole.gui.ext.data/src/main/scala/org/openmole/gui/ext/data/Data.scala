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
  def extension: String
  def displayable: Boolean
}

//trait OpenMOLEScript

case class DisplayableFile(extension: String, highlighter: String, displayable: Boolean = true) extends FileExtension

case class OpenMOLEScript(extension: String, highlighter: String, displayable: Boolean = true) extends FileExtension

case class BinaryFile(extension: String, displayable: Boolean = false) extends FileExtension

object FileExtension {
  val OMS = OpenMOLEScript("oms", "scala")
  val SCALA = DisplayableFile("scala", "scala")
  val NETLOGO = DisplayableFile("nlogo", "nlogo")
  val SH = DisplayableFile("sh", "sh")
  val NO_EXTENSION = DisplayableFile("text", "text")
  val BINARY = BinaryFile("")
}

object SafePath {
  def sp(path: String, leaf: String, extension: FileExtension = FileExtension.NO_EXTENSION): SafePath =
    SafePath(new URI(path).toString, leaf, extension)

  def leaf(name: String, extension: FileExtension) = sp(name, name, extension)

  def empty = leaf("", FileExtension.NO_EXTENSION)
}

import SafePath._

case class SafePath(encoded: String, leaf: String, extension: FileExtension) {
  val path = new URI(encoded).getPath

  def /(safePath: SafePath) = sp(this.path + "/" + safePath.path,
    safePath.leaf, safePath.extension)
}

sealed trait AuthenticationData extends Data {
  def synthetic: String
}

case class LoginPasswordAuthenticationData(login: String = "",
                                           cypheredPassword: String = "",
                                           target: String = "") extends AuthenticationData {
  def synthetic = s"$login@$target"
}

case class PrivateKeyAuthenticationData(privateKey: SafePath = SafePath.empty,
                                        login: String = "",
                                        cypheredPassword: String = "",
                                        target: String = "") extends AuthenticationData {
  def synthetic = s"$login@$target"
}

case class EGIP12AuthenticationData(val cypheredPassword: String = "",
                                    val certificatePath: SafePath = SafePath.empty) extends AuthenticationData {
  def synthetic = "egi.p12"
}

@JSExport
case class TreeNodeData(
  name: String,
  canonicalPath: SafePath,
  isDirectory: Boolean,
  size: Long,
  readableSize: String)

@JSExport
case class ScriptData(
  scriptName: String,
  script: String,
  inputDirectory: String,
  outputDirectory: String,
  output: String)

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

case class StaticExecutionInfo(name: String = "", script: String = "", startDate: Long = 0L)

case class EnvironmentState(taskName: String, running: Long, done: Long, submitted: Long, failed: Long)

//case class Output(output: String)

sealed trait ExecutionInfo {
  def state: String

  def duration: Long

  def completed: Long
}

case class Failed(error: Error, duration: Long = 0L, completed: Long = 0L, lastOutputs: String = "") extends ExecutionInfo {
  def state: String = "failed"
}

case class Running(ready: Long, running: Long, duration: Long, completed: Long, environmentStates: Seq[EnvironmentState], lastOutputs: String) extends ExecutionInfo {
  def state: String = "running"
}

case class Finished(duration: Long = 0L, completed: Long = 0L, lastOutputs: String) extends ExecutionInfo {
  def state: String = "finished"
}

case class Canceled(duration: Long = 0L, completed: Long = 0L, lastOutputs: String) extends ExecutionInfo {
  def state: String = "canceled"
}

case class Ready() extends ExecutionInfo {
  def state: String = "ready"

  def duration: Long = 0L

  def completed: Long = 0L
}

@JSExport
case class PasswordState(chosen: Boolean, hasBeenSet: Boolean)

