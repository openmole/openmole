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
  val id: String = java.util.UUID.randomUUID.toString
  def name: String
}

trait PrototypeData[T] extends Data

trait TaskData extends Data {
  //inputs with optionaly a default value
  def inputs: Seq[(PrototypeData[_], Option[String])]
  //outputs
  def outputs: Seq[PrototypeData[_]]

}

case class ErrorData(data: Data, error: String, stack: String)