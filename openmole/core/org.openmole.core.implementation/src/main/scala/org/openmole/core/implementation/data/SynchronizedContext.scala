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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.data

import org.openmole.commons.tools.pattern.IVisitor
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable


class SynchronizedContext(context: IContext) extends IContext {
  
  def this() = this(new Context)
  
  override def variables: Map[String, IVariable[_]] = synchronized{context.variables}
    
  override def ++=(vars: Iterable[IVariable[_]]): this.type = {synchronized{context ++= vars}; this}
  
  override def +=(variable: IVariable[_]): this.type = {synchronized{context += variable}; this}
    
  override def +=(name: String, value: Object): this.type = {synchronized{context += (name,value)}; this}
    
  override def +=[T](name: String, t: Class[T], value: T): this.type = {synchronized{context += (name,t,value)}; this}
 
  override def +=[T] (proto: IPrototype[T], value: T): this.type = {synchronized{context += (proto,value)}; this}

  override def -=(name: String): this.type = {synchronized{context -= name};this}
  
  override def value[T](name: String): Option[T] = synchronized{context.value(name)}

  override def value[T](proto: IPrototype[T]) = synchronized{context.value(proto)}
  
  override def variable[T](proto: IPrototype[T]): Option[IVariable[T]] = synchronized(context.variable(proto))
    
  override def variable[T](name: String): Option[IVariable[T]] = synchronized(context.variable(name))
 
  override def containsVariableWithName(name: String): Boolean = synchronized{context.containsVariableWithName(name)}
    
  override def containsVariableWithName(proto: IPrototype[_]): Boolean = synchronized{context.containsVariableWithName(proto)}

  override def contains(proto: IPrototype[_]): Boolean = synchronized{context.contains(proto)}
   
  override def clean = synchronized{context.clean}

  override def visit(visitor: IVisitor[IVariable[_]]) = synchronized{context.visit(visitor)}

  override def iterator: Iterator[IVariable[_]] = synchronized{context.iterator}
}
