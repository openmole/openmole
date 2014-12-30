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

trait Data {
  val uuid: String = java.util.UUID.randomUUID.toString
  def name: String
}

object ProtoTYPE extends Enumeration {
  case class ProtoTYPE(name: String, uuid: String = java.util.UUID.randomUUID.toString) extends Val(name)
  val INT = new ProtoTYPE("Integer")
  val DOUBLE = new ProtoTYPE("Double")
  val LONG = new ProtoTYPE("Long")
  val BOOLEAN = new ProtoTYPE("Boolean")
  val STRING = new ProtoTYPE("java.lang.String")
  val FILE = new ProtoTYPE("java.io.File")
  val ALL = Seq(INT, DOUBLE, LONG, BOOLEAN, STRING, FILE)
}

import ProtoTYPE._
class PrototypeData(val name: String, val `type`: ProtoTYPE, val dimension: Int) extends Data

object PrototypeData {
  def apply(name: String, `type`: ProtoTYPE, dimension: Int) = new PrototypeData(name, `type`, dimension)
  def integer(name: String, dimension: Int) = new PrototypeData(name, INT, dimension)
  def double(name: String, dimension: Int) = new PrototypeData(name, DOUBLE, dimension)
  def long(name: String, dimension: Int) = new PrototypeData(name, LONG, dimension)
  def boolean(name: String, dimension: Int) = new PrototypeData(name, BOOLEAN, dimension)
  def string(name: String, dimension: Int) = new PrototypeData(name, STRING, dimension)
  def file(name: String, dimension: Int) = new PrototypeData(name, FILE, dimension)

}

trait TaskData extends Data {
  def inputs: Seq[(PrototypeData, Option[String])]
  def outputs: Seq[PrototypeData]
}

case class ErrorData(data: Data, error: String, stack: String)