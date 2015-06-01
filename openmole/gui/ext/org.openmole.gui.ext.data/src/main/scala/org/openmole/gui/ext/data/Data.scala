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

@JSExport
case class TreeNodeData(
  name: String,
  canonicalPath: String,
  isDirectory: Boolean,
  size: Long,
  readableSize: String)

@JSExport
case class ScriptData(
  script: String,
  inputDirectory: String,
  outputDirectory: String,
  output: String)

object Error {
  def apply(e: Throwable): Error = {
    val sw = new StringWriter()
    e.printStackTrace(new PrintWriter(sw))
    Error(e.getMessage, Some(sw.toString))
  }
}

case class Error(message: String, stackTrace: Option[String] = None)

case class Token(token: String, duration: Long)

@JSExport("message.ExecutionId")
case class ExecutionId(id: String = java.util.UUID.randomUUID.toString)

case class Output(output: String)

object ExecutionState {
  type ExecutionState = String
  val running: ExecutionState = "running"
  val finished: ExecutionState = "finished"
  val failed: ExecutionState = "failed"
}

sealed trait State {
  def state: ExecutionState.ExecutionState
}

case class Failed(error: Error, state: ExecutionState.ExecutionState = ExecutionState.failed) extends State

case class Running(ready: Long, running: Long, completed: Long, state: ExecutionState.ExecutionState = ExecutionState.running) extends State

case class Finished(state: ExecutionState.ExecutionState = ExecutionState.finished) extends State

case class States(running: Running, finished: Finished)