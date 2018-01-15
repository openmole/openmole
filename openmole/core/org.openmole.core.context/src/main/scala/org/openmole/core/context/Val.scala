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
import shapeless.Typeable

import scala.annotation.tailrec
import scala.reflect._

object ValType {

  def unArrayify(t: ValType[_]): (ValType[_], Int) = {
    @tailrec def rec(c: ValType[_], level: Int = 0): (ValType[_], Int) =
      if (!c.isArray) (c, level)
      else rec(c.asArray.fromArray, level + 1)
    rec(t)
  }

  implicit class ValTypeDecorator[T](p: ValType[T]) {
    def toArray = ValType[Array[T]](p.manifest.toArray)
    def array = toArray
    def isArray = p.manifest.isArray
    def asArray = p.asInstanceOf[ValType[Array[T]]]
  }

  implicit class ValTypeArrayDecorator[T](p: ValType[Array[T]]) {
    def fromArray = ValType[T](p.manifest.fromArray)
  }

  implicit def buildValType[T: Manifest]: ValType[T] = ValType[T]

  def apply[T](implicit m: Manifest[T]): ValType[T] =
    new ValType[T] {
      val manifest = m
    }

  def unsecure(c: Manifest[_]): ValType[Any] = apply(c.asInstanceOf[Manifest[Any]])

}

trait ValType[T] extends Id {
  def manifest: Manifest[T]
  override def toString = manifest.toString
  def id = manifest
  def runtimeClass = manifest.runtimeClass
}

object Val {

  implicit class ValToArrayDecorator[T](prototype: Val[T]) {
    def toArray(level: Int): Val[_] = {
      def toArrayRecursive[A](prototype: Val[A], level: Int): Val[_] = {
        if (level <= 0) prototype
        else {
          val arrayProto = Val(prototype.name)(prototype.`type`.toArray).asInstanceOf[Val[Array[_]]]
          if (level <= 1) arrayProto
          else toArrayRecursive(arrayProto, level - 1)
        }
      }

      toArrayRecursive(prototype, level)
    }

    def toArray: Val[Array[T]] = Val(prototype.name)(prototype.`type`.toArray)

    def array(level: Int) = toArray(level)
    def array = toArray

    def unsecureType = prototype.`type`.asInstanceOf[Manifest[Any]]
  }

  implicit class ValFromArrayDecorator[T](prototype: Val[Array[T]]) {
    def fromArray: Val[T] = Val(prototype.name)(prototype.`type`.fromArray)
  }

  implicit def valDecorator[T](v: Val[T]) = new {
    def withName(name: String) = Val[T](name)(v.`type`)
  }

  implicit def valToArrayConverter[T](p: Val[T]) = p.toArray

  def apply[T](name: String, namespace: Namespace = Namespace.empty)(implicit t: ValType[T]): Val[T] = {
    assert(t != null)
    new Val[T](name, t, namespace)
  }

  def apply[T](implicit t: ValType[T], name: sourcecode.Name): Val[T] = apply[T](name.value)

  implicit lazy val valOrderingOnName = new Ordering[Val[_]] {
    override def compare(left: Val[_], right: Val[_]) =
      left.name compare right.name
  }

  implicit def isTypeable[T: Manifest]: Typeable[Val[T]] =
    new Typeable[Val[T]] {
      def cast(t: Any): Option[Val[T]] =
        t match {
          case v: Val[_] if v.`type`.manifest == manifest[T] ⇒ Some(v.asInstanceOf[Val[T]])
          case _ ⇒ None
        }
      def describe = s"Val[${manifest[T].toString}]"
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
class Val[T](val simpleName: String, val `type`: ValType[T], val namespace: Namespace) extends Id {
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
  def isAssignableFrom(p: Val[_]): Boolean = ClassUtils.assignable(p.`type`.manifest, `type`.manifest)

  def accepts(obj: Any): Boolean =
    obj == null || classAssignable(obj.getClass, `type`.runtimeClass)

  def withName(name: String) = Val[T](name, namespace = namespace)(`type`)
  def withType[T: ValType] = Val[T](simpleName, namespace = namespace)
  def withNamespace(namespace: Namespace) = Val[T](name, namespace = namespace)(`type`)

  def from(context: ⇒ Context): T = context(this)

  override def id = (name, `type`)
  override def toString = s"($name: ${`type`.toString})"
}

