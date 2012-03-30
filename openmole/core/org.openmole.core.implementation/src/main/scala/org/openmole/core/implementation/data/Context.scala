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

package org.openmole.core.implementation.data

import org.openmole.core.model.data.{IContext,IVariable,IPrototype}
import scala.collection.immutable.TreeMap

object Context {
  
  lazy val empty = new Context(TreeMap.empty)
  
  def apply(variables: IVariable[_]*): Context = apply(variables.toIterable)
  def apply(variables: Iterable[IVariable[_]]): Context = new Context(TreeMap.empty[String, IVariable[_]] ++ variables.map{v => v.prototype.name -> v})
  def apply(context: IContext, variables: java.lang.Iterable[IVariable[_]]) = {
    import collection.JavaConversions._
    context ++ variables
  }
  
  implicit def decorateVariableIterable(variables: Iterable[IVariable[_]]) = new {
    def toContext = Context(variables)
  }
  
}

class Context(val variables: TreeMap[String, IVariable[_]]) extends IContext {

  def this() = this(TreeMap.empty)
  
  override def ++(vars: Traversable[IVariable[_]]): IContext = new Context(variables ++ vars.map{v => v.prototype.name -> v})

  override def +(variable: IVariable[_]): IContext = new Context(variables + (variable.prototype.name -> variable))
    
  override def +(name: String, value: Object): IContext = this + new Variable[Object](name, value)
    
  override def +[T](name: String, t: Class[T], value: T): IContext = this + new Variable[T](name, t, value)
 
  override def +[T] (proto: IPrototype[T], value: T): IContext = this + (new Variable[T](proto, value))

  override def -(name: String): IContext = new Context(variables - name)
    
  override def --(names: Traversable[String]): IContext = new Context(variables -- names)
  
  override def value[T](name: String): Option[T] = variables.get(name) match {
    case None => None
    case Some(v) => Some(v.asInstanceOf[IVariable[T]].value)
  }

  override def value[T](proto: IPrototype[T]) = value[T](proto.name)

  
  override def variable[T](proto: IPrototype[T]): Option[IVariable[T]] = {
    variables.get(proto.name) match {
      case None => None
      case Some(v) => Some(v.asInstanceOf[IVariable[T]])
    }
  }
    
  override def variable[T](name: String): Option[IVariable[T]] = variables.get(name).asInstanceOf[Option[IVariable[T]]]
 
  override def containsVariableWithName(name: String): Boolean = variables.contains(name)
    
  override def containsVariableWithName(proto: IPrototype[_]): Boolean = containsVariableWithName(proto.name)

  override def contains(proto: IPrototype[_]): Boolean = {
    variable(proto.name) match {
      case None => false
      case Some(v) => proto.isAssignableFrom(v.prototype)
    }
  }


  override def iterator: Iterator[IVariable[_]] = variables.values.iterator
    
}
