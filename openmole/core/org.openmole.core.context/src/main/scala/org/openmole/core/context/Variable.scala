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

  /**
   * implicit conversion of a tuple (prototype,value) to a Variable
   * @param t
   * @tparam T
   * @return
   */
  implicit def tupleWithValToVariable[@specialized T](t: (Val[T], T)): Variable[T] = apply(t._1, t._2)

  /**
   * implicit conversion of tuple (prototype name, value)
   * @param t
   * @tparam T
   * @return
   */
  implicit def tupleToVariable[@specialized T: Manifest](t: (String, T)): Variable[T] = apply(Val[T](t._1), t._2)

  /**
   * Unsecure constructor, trying to cast the provided value to the type of the prototype
   * @param p prototype
   * @param v value
   * @tparam T
   * @return
   */
  def unsecure[@specialized T](p: Val[T], v: Any): Variable[T] = Variable[T](p, v.asInstanceOf[T])

  /**
   * Seed for rng
   */
  val openMOLESeed = Val[Long]("seed", namespace = openMOLENameSpace)

  def copy[@specialized T](v: Variable[T])(prototype: Val[T] = v.prototype, value: T = v.value): Variable[T] = apply(prototype, value)
}

/**
 * A Variable is a prototype with a value
 * @param prototype the prototype
 * @param value the value
 * @tparam T type of the Variable
 */
case class Variable[@specialized T](prototype: Val[T], value: T) {
  override def toString: String = prettified(Int.MaxValue)
  def prettified(snipArray: Int) = prototype.name + "=" + (if (value != null) value.prettify(snipArray) else "null")
  def name = prototype.name
}

