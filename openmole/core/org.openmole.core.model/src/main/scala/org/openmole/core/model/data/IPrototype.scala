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

/**
 * {@link IPrototype} is a prototype in the sens of C language prototypes. It is
 * composed of a type and a name. It allows specifying typed data transfert in
 * OpenMOLE.
 * 
 * @type T the type of the prototype. Values associated to this prototype should
 * always be a subtype of T.
 */
trait IPrototype[T] {
  
  /**
   * Get the name of the prototype.
   *
   * @return the name of the prototype
   */
  def name: String

  /**
   * Get the type of the prototype.
   *
   * @return the type of the prototype
   */
  def `type`: Manifest[T]

  /**
   * Test if this prototype can be assigned from another prototype. This work 
   * in the same way as java.lang.Class.isAssignableFrom.
   *
   * @param prototype the prototype to test
   * @return true if the prototype is assignable from the given prototype
   */
  def isAssignableFrom(prototype: IPrototype[_]): Boolean

  def accepts(obj: Any): Boolean
  /*override def equals(obj: Any): Boolean = (name, `type`).equals(obj)

  override def hashCode: Int = (name, `type`).hashCode*/
}

