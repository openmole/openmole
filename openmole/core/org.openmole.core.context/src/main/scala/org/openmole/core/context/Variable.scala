/*
 * Copyright (C) 2010 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.context

import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workspace.Workspace
import org.openmole.tool.random
import shapeless.Typeable

import scala.reflect.ClassTag
import scala.util.Random

object Variable {
  def openMOLENameSpace = Namespace("openmole")

  implicit def tupleWithValToVariable[T](t: (Val[T], T)) = apply(t._1, t._2)
  implicit def tubleToVariable[T: Manifest](t: (String, T)) = apply(Val[T](t._1), t._2)

  def unsecure[T](p: Val[T], v: Any) = Variable[T](p, v.asInstanceOf[T])

  def openMOLE(name: String) = Val[Long](name, namespace = openMOLENameSpace)
  val openMOLESeed = openMOLE("seed")

  def copy[T](v: Variable[T])(prototype: Val[T] = v.prototype, value: T = v.value) = apply(prototype, value)
}

case class Variable[T](prototype: Val[T], value: T) {
  override def toString: String = prettified(Int.MaxValue)
  def prettified(snipArray: Int) = prototype.name + "=" + (if (value != null) value.prettify(snipArray) else "null")
}

