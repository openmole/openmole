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

import org.openmole.core.model.data.{IContext,IVariable,IPrototype}
import scala.collection.immutable.TreeMap

class Context extends IContext {

  var _variables = new TreeMap[String, IVariable[_]]
  
  override def variables: Map[String, IVariable[_]] = _variables
    
  override def ++=(vars: Iterable[IVariable[_]]): this.type = {for (variable <- vars)  this.+=(variable); this}

  override def +=(variable: IVariable[_]): this.type = {_variables += ((variable.prototype.name, variable)); this}
    
  override def +=(name: String, value: Object): this.type = {+= (new Variable[Object](name, value))}
    
  override def +=[T](name: String, t: Class[T], value: T): this.type = {+= (new Variable[T](name, t, value))}
 
  override def +=[T] (proto: IPrototype[T], value: T): this.type = {+= (new Variable[T](proto, value))}

  override def -=(name: String): this.type = {_variables -= name; this}
  
  override def value[T](name: String): Option[T] = _variables.get(name) match {
    case None => None
    case Some(v) => Some(v.asInstanceOf[IVariable[T]].value)
  }

  override def value[T](proto: IPrototype[T]) = value[T](proto.name)

  
  override def variable[T](proto: IPrototype[T]): Option[IVariable[T]] = {
    _variables.get(proto.name) match {
      case None => None
      case Some(v) => Some(v.asInstanceOf[IVariable[T]])
    }
  }
    
  override def variable[T](name: String): Option[IVariable[T]] = _variables.get(name).asInstanceOf[Option[IVariable[T]]]
 
  override def containsVariableWithName(name: String): Boolean = _variables.contains(name)
    
  override def containsVariableWithName(proto: IPrototype[_]): Boolean = containsVariableWithName(proto.name)

  override def contains(proto: IPrototype[_]): Boolean = {
    variable(proto.name) match {
      case None => false
      case Some(v) => proto.`type`.isAssignableFrom(v.prototype.`type`)
    }
  }

  override def clean = {_variables = new TreeMap[String, IVariable[_]]}

  override def iterator: Iterator[IVariable[_]] = _variables.values.iterator
    
}
