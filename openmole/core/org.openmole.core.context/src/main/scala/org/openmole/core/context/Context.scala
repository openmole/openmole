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

import org.openmole.core.exception._
import org.openmole.core.preference._
import org.openmole.core.workspace._
import org.openmole.tool.random

import scala.collection._
import scala.collection.immutable.TreeMap

object Context {

  def ErrorArraySnipSize = ConfigurationLocation[Int]("Display", "ErrorArraySnipSize", Some(10))

  implicit def variableToContextConverter(variable: Variable[_]) = Context(variable)
  implicit def variablesToContextConverter(variables: Iterable[Variable[_]]): Context = Context(variables.toSeq: _*)

  def fromMap(v: Traversable[(String, Variable[_])]) = new Context {
    val variables = TreeMap.empty[String, Variable[_]] ++ v
  }

  def apply(v: T forSome { type T <: Variable[_] }*): Context = Context.fromMap(v.map { v ⇒ v.prototype.name → v })

  val empty = apply()

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
   * @param p the prototype that matches the name of the variable
   * @return Some(variable) if a variable with the given name is present None
   * otherwise
   */
  def variable[T](p: Val[T]): Option[Variable[T]] = variables.get(p.name).map(_.asInstanceOf[Variable[T]])

  /**
   * Get a variable value given its name.
   *
   * @param name the name of the variable
   * @return Some(value) if a variable with the given name is present None
   * otherwise
   */
  def option[T](name: String): Option[T] = variables.get(name).map(_.asInstanceOf[Variable[T]].value)

  /**
   * Get a variable value given a prototype name. This method get the variable by its
   * name and then cast it to the correct type.
   *
   * @param proto the prototype that matches the name of the variable
   * @return Some(value) if a variable with the given name is present None
   * otherwise
   */
  def option[T](proto: Val[T]): Option[T] = option[T](proto.name)

  def getOrElse[T](v: Val[T], f: ⇒ T): T = option(v).getOrElse(f)

  /**
   * Get a variable valaue given a prototype name. This method get the variable by its
   * name and then cast it to the correct type.
   *
   * @param name the name of the variable
   * @return value the value
   * @throws a UserBadDataError if the variable hasn't been found
   */
  def apply[T](name: String): T = option(name).getOrElse(throw new UserBadDataError(s"Variable $name has not been found in the context"))

  /**
   * Get a variable valaue given a prototype name. This method get the variable by its
   * name and then cast it to the correct type.
   *
   * @param p the prototype that matches the name of the variable
   * @return value the value
   * @throws a UserBadDataError if the variable hasn't been found
   */
  def apply[T](p: Val[T]): T = option(p).getOrElse(throw new UserBadDataError(s"Variable $p has not been found in the context"))

  /**
   * Build a new context containing the variables of the current context plus the
   * variable constructed from the parameters.
   *
   * @param p the prototype of the variable
   * @param value the value of the variable
   * @return the new context
   */
  def +[T](p: Val[T], value: T) = Context.fromMap(variables + (p.name → Variable[T](p, value)))

  /**
   * Build a new context containing the variables of the current context plus the
   * variable constructed from the parameters.
   *
   * @param tuple a tuple (prototype, value) to construct the variable
   * @return the new context
   */
  def +[T](tuple: (Val[T], T)): Context = this.+(tuple._1, tuple._2)

  def +[T](v: Variable[T]) = Context.fromMap(variables + (v.prototype.name → v))

  def +(ctx: Context) = Context.fromMap(variables ++ ctx)

  override def +[B1 >: Variable[_]](kv: (String, B1)) = variables + kv

  /**
   * Build a new context containing the variables of the current context plus the
   * variables given in parameter.
   *
   * @param vs the variables to add
   * @return the new context
   */

  def ++(vs: Traversable[Variable[_]]): Context = Context.fromMap(variables ++ (vs.map { v ⇒ v.prototype.name → v }))

  /**
   * Build a new context containing the variables of the current context minus the
   * variable which names have been passed in parameter.
   *
   * @param names the names of the variables
   * @return the new context
   */
  def --(names: Traversable[String]): Context = Context.fromMap(variables -- names)

  def contains(p: Val[_]): Boolean =
    variable(p.name) match {
      case None    ⇒ false
      case Some(v) ⇒ p.isAssignableFrom(v.prototype)
    }

  def -(name: String): Context = Context.fromMap(variables - name)
  def -(v: Val[_]): Context = this - v.name

  override def empty = Context.empty

  def get(key: String) = variables.get(key)
  def get[T](proto: Val[T]): Option[T] = option[T](proto)

  def update[T](p: Val[T], v: T) = this + Variable(p, v)

  override def iterator = variables.iterator

  override def toString = prettified(Int.MaxValue)

  def prototypes = values.map { _.prototype }

  def prettified(stripSize: Int): String =
    "{" + (if (variables.values.isEmpty) ""
    else variables.values.map(v ⇒ if (v != null) v.prettified(stripSize) else "null").mkString(", ")) + "}"

  def prettified(implicit preference: Preference): String = prettified(preference(Context.ErrorArraySnipSize))

}
