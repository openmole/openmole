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

import org.openmole.core.tools.obj.ClassUtils._
import org.openmole.core.tools.obj.{ ClassUtils, Id }

import scala.annotation.tailrec
import scala.reflect._

object PrototypeType {

  def unArrayify(t: PrototypeType[_]): (PrototypeType[_], Int) = {
    @tailrec def rec(c: PrototypeType[_], level: Int = 0): (PrototypeType[_], Int) =
      if (!c.isArray) (c, level)
      else rec(c.asArray.fromArray, level + 1)
    rec(t)
  }

  implicit class PrototypeTypeDecorator[T](p: PrototypeType[T]) {
    def toArray = PrototypeType[Array[T]](p.manifest.toArray)
    def array = toArray
    def isArray = p.manifest.isArray
    def asArray = p.asInstanceOf[PrototypeType[Array[T]]]
  }

  implicit class PrototypeTypeArrayDecorator[T](p: PrototypeType[Array[T]]) {
    def fromArray = PrototypeType[T](p.manifest.fromArray)
  }

  implicit def buildPrototypeType[T: Manifest]: PrototypeType[T] = PrototypeType[T]

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

  implicit class PrototypeToArrayDecorator[T](prototype: Prototype[T]) {
    def toArray(level: Int): Prototype[_] = {
      def toArrayRecursive[A](prototype: Prototype[A], level: Int): Prototype[_] = {
        if (level <= 0) prototype
        else {
          val arrayProto = Prototype(prototype.name)(prototype.`type`.toArray).asInstanceOf[Prototype[Array[_]]]
          if (level <= 1) arrayProto
          else toArrayRecursive(arrayProto, level - 1)
        }
      }

      toArrayRecursive(prototype, level)
    }

    def toArray: Prototype[Array[T]] = Prototype(prototype.name)(prototype.`type`.toArray)

    def array(level: Int) = toArray(level)
    def array = toArray

    def unsecureType = prototype.`type`.asInstanceOf[Manifest[Any]]
  }

  implicit class PrototypeFromArrayDecorator[T](prototype: Prototype[Array[T]]) {
    def fromArray: Prototype[T] = Prototype(prototype.name)(prototype.`type`.fromArray)
  }

  implicit def prototypeDecorator[T](prototype: Prototype[T]) = new {
    def withName(name: String) = Prototype[T](name)(prototype.`type`)
  }

  implicit def prototypeToArrayConverter[T](p: Prototype[T]) = p.toArray

  def apply[T](name: String, namespace: Namespace = Namespace.empty)(implicit t: PrototypeType[T]): Prototype[T] = {
    assert(t != null)
    new Prototype[T](name, t, namespace)
  }

  implicit lazy val prototypeOrderingOnName = new Ordering[Prototype[_]] {
    override def compare(left: Prototype[_], right: Prototype[_]) =
      left.name compare right.name
  }

}

object Namespace {
  def empty = Namespace()
}

case class Namespace(names: String*) {
  override def toString =
    if (names.isEmpty) ""
    else names.mkString("$") + "$"
}

/**
 * {@link Prototype} is a prototype in the sens of C language prototypes. It is
 * composed of a type and a name. It allows specifying typed data transfert in
 * OpenMOLE.
 *
 * @tparam T the type of the prototype. Values associated to this prototype should
 * always be a subtype of T.
 */
class Prototype[T](val simpleName: String, val `type`: PrototypeType[T], val namespace: Namespace) extends Id {
  /**
   * Get the name of the prototype.
   *
   * @return the name of the prototype
   */
  def name: String = namespace.toString + simpleName

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

  def from(context: ⇒ Context): T = context(this)

  override def id = (name, `type`)
  override def toString = s"($name: ${`type`.toString})"
}

