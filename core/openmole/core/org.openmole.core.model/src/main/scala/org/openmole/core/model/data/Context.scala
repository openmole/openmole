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

package org.openmole.core.model.data

import org.openmole.misc.exception.UserBadDataError
import scala.collection.immutable.MapLike
import scala.collection.immutable.TreeMap
import org.openmole.misc.workspace.Workspace

object Context {

  def fromMap(v: Traversable[(String, Variable[_])]) = new Context {
    val variables = TreeMap.empty[String, Variable[_]] ++ v
  }

  def apply(v: Variable[_]*): Context = apply(v)
  def apply(v: Traversable[Variable[_]]): Context = Context.fromMap(v.map { v ⇒ v.prototype.name -> v })

  val empty = apply(Iterable.empty)

}

/**
 * Context represents a bunch of variables used by the task excutions.
 * A task execution can remove variables from context, change the values of
 * the variables and add values to it.
 */
trait Context extends Map[String, Variable[_]] with MapLike[String, Variable[_], Context] {

  def variables: TreeMap[String, Variable[_]]

  /**
   * Get a variable given its name.
   *
   * @param name the name of the variable
   * @return Some(variable) if a variable with the given name is present None
   * otherwise
   */
  def variable[T](name: String): Option[Variable[T]] = variables.get(name).asInstanceOf[Option[Variable[T]]]

  /**
   * Get a variable given a prototype name. This method get the variable by its
   * name and then cast it to the correct type.
   *
   * @param proto the prototype that matches the name of the variable
   * @return Some(variable) if a variable with the given name is present None
   * otherwise
   */
  def variable[T](p: Prototype[T]): Option[Variable[T]] = variables.get(p.name).map(_.asInstanceOf[Variable[T]])

  /**
   * Get a variable value given its name.
   *
   * @param name the name of the variable
   * @return Some(value) if a variable with the given name is present None
   * otherwise
   */
  @deprecated("Use option instead") def value[T](name: String): Option[T] = option[T](name)
  def option[T](name: String): Option[T] = variables.get(name).map(_.asInstanceOf[Variable[T]].value)

  /**
   * Get a variable value given a prototype name. This method get the variable by its
   * name and then cast it to the correct type.
   *
   * @param proto the prototype that matches the name of the variable
   * @return Some(value) if a variable with the given name is present None
   * otherwise
   */
  @deprecated("Use option instead") def value[T](proto: Prototype[T]): Option[T] = option[T](proto)
  def option[T](proto: Prototype[T]): Option[T] = option[T](proto.name)

  /**
   * Get a variable valaue given a prototype name. This method get the variable by its
   * name and then cast it to the correct type.
   *
   * @param proto the prototype that matches the name of the variable
   * @return value the value
   * @throws a UserBadDataError if the variable hasn't been found
   */
  @deprecated("Use apply instead") def valueOrException[T](name: String): T = apply(name)
  def apply[T](name: String): T = value(name).getOrElse(throw new UserBadDataError("Variable " + name + " has not been found in the context"))

  /**
   * Get a variable valaue given a prototype name. This method get the variable by its
   * name and then cast it to the correct type.
   *
   * @param proto the prototype that matches the name of the variable
   * @return value the value
   * @throws a UserBadDataError if the variable hasn't been found
   */
  @deprecated("Use apply instead") def valueOrException[T](proto: Prototype[T]): T = apply[T](proto)
  def apply[T](proto: Prototype[T]): T = value(proto).getOrElse(throw new UserBadDataError("Variable " + proto + " has not been found in the context"))

  /**
   * Build a new context containing the variables of the current context plus the
   * variable constructed from the parameters.
   *
   * @param prototype the prototype of the variable
   * @param value the value of the variable
   * @return the new context
   */
  def +[T](p: Prototype[T], value: T) = Context.fromMap(variables + (p.name -> Variable[T](p, value)))

  /**
   * Build a new context containing the variables of the current context plus the
   * variable constructed from the parameters.
   *
   * @param tuple a tuple (prototype, value) to construct the variable
   * @return the new context
   */
  def +[T](tuple: (Prototype[T], T)): Context = this.+(tuple._1, tuple._2)

  def +[T](v: Variable[T]) = Context.fromMap(variables + (v.prototype.name -> v))

  def +(ctx: Context) = Context.fromMap(variables ++ ctx)

  override def +[B1 >: Variable[_]](kv: (String, B1)) = variables + kv

  /**
   * Build a new context containing the variables of the current context plus the
   * variables given in parameter.
   *
   * @param variables the variables to add
   * @return the new context
   */

  def ++(vs: Traversable[Variable[_]]): Context = Context.fromMap(variables ++ (vs.map { v ⇒ v.prototype.name -> v }))

  /**
   * Build a new context containing the variables of the current context minus the
   * variable which names have been passed in parameter.
   *
   * @param names the names of the variables
   * @return the new context
   */
  def --(names: Traversable[String]): Context = Context.fromMap(variables -- names)

  def contains(p: Prototype[_]): Boolean =
    variable(p.name) match {
      case None ⇒ false
      case Some(v) ⇒ p.isAssignableFrom(v.prototype)
    }

  def -(name: String) = Context.fromMap(variables - name)

  override def empty = Context.empty

  def get(key: String) = variables.get(key)

  override def iterator = variables.iterator

  override def toString = prettified(Int.MaxValue)

  def prototypes = values.map { _.prototype }

  def prettified(stripSize: Int = Workspace.preferenceAsInt(Workspace.ErrorArraySnipSize)) =
    "{" + (if (variables.values.isEmpty) ""
    else variables.values.map(v ⇒ if (v != null) v.prettified(stripSize) else v).mkString(", ")) + "}"

}
