/*
 * Copyright (C) 2010 reuillon
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

/**
 * IContext represents a bunch of variables used by the task excutions. 
 * A task execution can remove variables from context, change the values of 
 * the variables and add values to it.
 */
import org.openmole.misc.exception.UserBadDataError

trait IContext extends Iterable[IVariable[_]] {
    
  /**
   * Get all the variables in this context.
   * 
   * @return all the variables in this context
   */
  def variables: Map[String, IVariable[_]]

  /** 
   * Get a variable given its name.
   * 
   * @param name the name of the variable
   * @return Some(variable) if a variable with the given name is present None
   * otherwise
   */
  def variable[T](name: String): Option[IVariable[T]]

  /**
   * Get a variable given a prototype name. This method get the variable by its
   * name and then cast it to the correct type.
   * 
   * @param proto the prototype that matches the name of the variable
   * @return Some(variable) if a variable with the given name is present None
   * otherwise
   */
  def variable[T] (proto: IPrototype[T]): Option[IVariable[T]]

  /** 
   * Get a variable value given its name.
   * 
   * @param name the name of the variable
   * @return Some(value) if a variable with the given name is present None
   * otherwise
   */
  def value[T](name: String): Option[T]

  /**
   * Get a variable value given a prototype name. This method get the variable by its
   * name and then cast it to the correct type.
   * 
   * @param proto the prototype that matches the name of the variable
   * @return Some(value) if a variable with the given name is present None
   * otherwise
   */
  def value[T](proto: IPrototype[T]): Option[T]
  
    /**
   * Get a variable valaue given a prototype name. This method get the variable by its
   * name and then cast it to the correct type.
   * 
   * @param proto the prototype that matches the name of the variable
   * @return value the value
   * @throws a UserBadDataError if the variable hasn't been found
   */
  def valueOrException[T](name: String): T = value(name).getOrElse(throw new UserBadDataError("Variable " + name + " has not been found in the context"))
  
  /**
   * Get a variable valaue given a prototype name. This method get the variable by its
   * name and then cast it to the correct type.
   * 
   * @param proto the prototype that matches the name of the variable
   * @return value the value
   * @throws a UserBadDataError if the variable hasn't been found
   */
  def valueOrException[T](proto: IPrototype[T]): T = value(proto).getOrElse(throw new UserBadDataError("Variable " + proto + " has not been found in the context"))

  
  /**
   * Build a new context containing the variables of the current context plus the 
   * variable give as parameter.
   * 
   * @param variable the variable to add
   * @return the new context
   */
  def +(variable: IVariable[_]): IContext

  /**
   * Build a new context containing the variables of the current context plus the 
   * variable constructed from the parameters.
   * 
   * @param name the name of the variable
   * @param value the value of the variable
   * @return the new context
   */
  def +(name: String, value: Object): IContext

  /**
   * Build a new context containing the variables of the current context plus the 
   * variable constructed from the parameters.
   * 
   * @param name the name of the variable
   * @param t the type of the variable
   * @param value the value of the variable
   * @return the new context
   */
  def +[T] (name: String, t: Class[T], value: T): IContext
    
  /**
   * Build a new context containing the variables of the current context plus the 
   * variable constructed from the parameters.
   * 
   * @param prototype the prototype of the variable
   * @param value the value of the variable
   * @return the new context
   */
  def +[T] (proto: IPrototype[T], value: T): IContext
  
  /**
   * Build a new context containing the variables of the current context plus the 
   * variable constructed from the parameters.
   * 
   * @param tuple a tuple (prototype, value) to construct the variable
   * @return the new context
   */
  def +[T] (tuple: (IPrototype[T],T)): IContext = this.+(tuple._1, tuple._2)

  /**
   * Build a new context containing the variables of the current context plus the 
   * variables given in parameter.
   *
   * @param variables the variables to add
   * @return the new context
   */
  def ++(variables: Traversable[IVariable[_]] ): IContext
  
  /**
   * Build a new context containing the variables of the current context minus the
   * variable which name has been passed in parameter.
   * 
   * @param name the name of the variable
   * @return the new context
   */
  def -(name: String): IContext
  
  /**
   * Build a new context containing the variables of the current context minus the
   * variable which names have been passed in parameter.
   * 
   * @param names the names of the variables
   * @return the new context
   */
  def --(name: Traversable[String]): IContext

  /**
   * Check if the context contains a variable with a given prototype's name.
   * 
   * @param proto a prototype with the same name as the variable
   * @return true if a variable with the given name as been found false
   * otherwise
   */
  def containsVariableWithName(proto: IPrototype[_]): Boolean
  
  /**
   * Check if the context contains a variable with a given name.
   * 
   * @param name the name of the variable
   * @return true if a variable with the given name as been found false
   * otherwise
   */
  def containsVariableWithName(name: String): Boolean

  /**
   * Check if a variable matching a given prototype is contained in the context.
   * A variable matches the prototype if and only if it has the same name and
   * the prototype type is assignable from the variable type.
   * 
   * @param proto the prototype to look for in the context
   * @return true if a variable matching the prototype has been found, false
   * otherwise
   */
  def contains(proto: IPrototype[_]): Boolean

}
