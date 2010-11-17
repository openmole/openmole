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

package org.openmole.core.model.data

import org.openmole.commons.tools.pattern.IVisitable

trait IContext extends IVisitable[IVariable[_]] with Iterable[IVariable[_]] {

    def ++=(variables: Iterable[IVariable[_]] ): this.type
    
    def addAll(variables: Iterable[IVariable[_]] ): this.type = { ++= (variables) }

    def variables: Map[String, IVariable[_]]

    def variable[T](name: String): Option[IVariable[T]]

    def variable[T] (proto: IPrototype[T]): Option[IVariable[T]]
     
    def +=(variable: IVariable[_]): this.type

    def +=(name: String, value: Object): this.type

    def +=[T] (name: String, t: Class[T], value: T): this.type
    
    def +=[T] (proto: IPrototype[T], value: T): this.type
  
    def add(variable: IVariable[_]): this.type = { += (variable)}

    def add(name: String, value: Object): this.type = { += (name, value)}

    def add[T] (name: String, t: Class[T], value: T): this.type = { += (name, t, value)}
  
    def add[T] (proto: IPrototype[T], value: T): this.type = { += (proto, value)}
 
    def value[T](name: String): Option[T]

    def value[T](proto: IPrototype[T]): Option[T]
 
    def -=(name: String): this.type
    
    def remove(name: String): this.type = { -= (name) }

    def containsVariableWithName(proto: IPrototype[_]): Boolean

    def containsVariableWithName(name: String): Boolean

    def contains(proto: IPrototype[_]): Boolean

    /**
     * Reset the content of the context. After this operation, the context doesn't
     * contain any variable, and is ready to use.
     */
    def clean
}
