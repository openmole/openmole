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

case class DataBag(uuid: String, data: Data, name: String)

trait Data

object ProtoTYPE extends Enumeration {
  case class ProtoTYPE(uuid: String, name: String) extends Val(name)
  val INT = new ProtoTYPE("Integer", "Integer")
  val DOUBLE = new ProtoTYPE("Double", "Double")
  val LONG = new ProtoTYPE("Long", "Long")
  val BOOLEAN = new ProtoTYPE("Boolean", "Boolean")
  val STRING = new ProtoTYPE("String", "java.lang.String")
  val FILE = new ProtoTYPE("File", "java.io.File")
  val ALL = Seq(INT, DOUBLE, LONG, BOOLEAN, STRING, FILE)
}

import ProtoTYPE._
class PrototypeData(val `type`: ProtoTYPE, val dimension: Int) extends Data

object PrototypeData {
  def apply(`type`: ProtoTYPE, dimension: Int) = new PrototypeData(`type`, dimension)
  def integer(dimension: Int) = new PrototypeData(INT, dimension)
  def double(dimension: Int) = new PrototypeData(DOUBLE, dimension)
  def long(dimension: Int) = new PrototypeData(LONG, dimension)
  def boolean(dimension: Int) = new PrototypeData(BOOLEAN, dimension)
  def string(dimension: Int) = new PrototypeData(STRING, dimension)
  def file(dimension: Int) = new PrototypeData(FILE, dimension)

}

trait TaskData extends Data {
  def inputs: Seq[(PrototypeData, Option[String])]
  def outputs: Seq[PrototypeData]
}

case class ErrorData(data: DataBag, error: String, stack: String)