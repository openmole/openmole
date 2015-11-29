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

package org.openmole.core.workflow.data

import org.openmole.core.macros.ExtractValName
import org.openmole.core.tools.obj.{ Id, ClassUtils }
import ClassUtils._
import org.openmole.core.tools.obj.ClassUtils
import scala.reflect._
import scala.reflect.runtime.universe._

object PrototypeType {

  def apply[T](implicit m: Manifest[T]): PrototypeType[T] =
    new PrototypeType[T] {
      val manifest = m
    }

  def unsecure(c: Manifest[_]): PrototypeType[Any] = apply(c.asInstanceOf[Manifest[Any]])

}

trait PrototypeType[T] extends Id {
  def manifest: Manifest[T]
  override def toString = manifest.toString
  def id = manifest
  def runtimeClass = manifest.runtimeClass
}

object Prototype {

  implicit def prototypeToArrayConverter[T](p: Prototype[T]) = p.toArray

  def apply[T](n: String)(implicit t: PrototypeType[T]): Prototype[T] = {
    assert(t != null)
    new Prototype[T] {
      val name = n
      val `type` = t
    }
  }

  implicit lazy val prototypeOrderingOnName = new Ordering[Prototype[_]] {
    override def compare(left: Prototype[_], right: Prototype[_]) =
      left.name compare right.name
  }

}

/**
 * {@link Prototype} is a prototype in the sens of C language prototypes. It is
 * composed of a type and a name. It allows specifying typed data transfert in
 * OpenMOLE.
 *
 * @type T the type of the prototype. Values associated to this prototype should
 * always be a subtype of T.
 */
trait Prototype[T] extends Id {

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
  def `type`: PrototypeType[T]

  /**
   * Test if this prototype can be assigned from another prototype. This work
   * in the same way as java.lang.Class.isAssignableFrom.
   *
   * @param p the prototype to test
   * @return true if the prototype is assignable from the given prototype
   */
  def isAssignableFrom(p: Prototype[_]): Boolean = ClassUtils.assignable(p.`type`.manifest, `type`.manifest)

  def accepts(obj: Any): Boolean =
    obj == null || classAssignable(obj.getClass, `type`.runtimeClass)

  def withName(name: String) = Prototype[T](name)(`type`)

  override def id = (name, `type`)
  override def toString = name + ": " + `type`.toString

}

