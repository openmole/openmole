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

import org.openmole.core.model.data.{IContext,IVariable,IPrototype, IData}
import scala.collection.immutable.TreeMap

object Context {
  
  val empty = new Context(TreeMap.empty)
  
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

class Context(val variables: TreeMap[String, IVariable[_]]) extends Map[String, IVariable[_]] with IContext {

  def this() = this(TreeMap.empty)

  override def +[B1 >: IVariable[_]](kv: (String, B1)) = variables + kv
  //override def +(name: String, variable: IVariable[_]) = new Context(variables + (name -> variable))
  override def -(name: String) = new Context(variables - name)
  
  //override def +(name: String, value: Object) = new Context(variables + (name -> new Variable[Object](name, value)))
    
  //override def +[T](name: String, t: Class[T], value: T) = new Context(variables + (name -> new Variable[T](name, t, value)))
 
  override def +[T] (p: IPrototype[T], value: T) = new Context(variables + (p.name -> new Variable[T](p, value)))
  
  override def +[T] (v: IVariable[T]) = new Context(variables + (v.prototype.name -> v))
  
  override def +(ctx: IContext) = new Context(variables ++ ctx)
  
  override def ++(vs: Traversable[IVariable[_]]): IContext = new Context(variables ++ (vs.map{v => v.prototype.name -> v}))
  
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
 
  override def contains(p: IPrototype[_]): Boolean = {
    variable(p.name) match {
      case None => false
      case Some(v) => p.isAssignableFrom(v.prototype)
    }
  }

  override def empty = Context.empty
 
  override def get(key: String) = variables.get(key)
  
  override def iterator = variables.iterator
  
  override def toString = "{" + (if(variables.values.isEmpty) "" else variables.values.mkString(", ")) + "}"
}
