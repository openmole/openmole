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

import org.openmole.core.exception.UserBadDataError
import org.openmole.tool.types.TypeTool._
import org.openmole.tool.types.{ TypeTool, Id }
import shapeless3.typeable.{ TypeCase, Typeable }

import scala.annotation.tailrec
import scala.reflect._

/**
 * Methods to deal with ValType
 */
object ValType:

  /**
   * Extract the atomic ValType of a (potentially multidimensional) Array
   * @param t
   * @return
   */
  def unArrayify(t: ValType[?]): (ValType[?], Int) =
    @tailrec def rec(c: ValType[?], level: Int = 0): (ValType[?], Int) =
      if (!c.isArray) (c, level)
      else rec(c.asArray.fromArray, level + 1)
    rec(t)

  def unsecureFromArray(t: ValType[?]): ValType[?] =
    val (res, level) = unArrayify(t)
    if (level == 0) throw new UserBadDataError(s"ValType $t is no an array type")
    res

  /**
   * Decorate ValType for implicit conversions to array type
   * @param p
   * @tparam T
   */
  implicit class ValTypeDecorator[T](p: ValType[T]):
    /**
     * convert to array
     * @return
     */
    def toArray = ValType[Array[T]](p.manifest.toArray)

    /**
     * alias of [[toArray]]
     * @return
     */
    def array = toArray

    /**
     * is it an array type
     * @return
     */
    def isArray = p.manifest.isArray

    /**
     * force conversion to array
     * @return
     */
    def asArray = p.asInstanceOf[ValType[Array[T]]]


  def fromArrayUnsecure(t: ValType[Array[?]]) = ValType[Any](manifestFromArrayUnsecure(t.manifest))

  /**
   * Decorate for conversion from array type
   * @param p
   * @tparam T
   */
  implicit class ValTypeArrayDecorator[T](t: ValType[Array[T]]):
    def fromArray = ValType[T](t.manifest.fromArray)

  implicit def buildValType[T: Manifest]: ValType[T] = ValType[T]

  /**
   * Construct a ValType from an implicit manifest
   * @param m
   * @tparam T
   * @return
   */
  def apply[T](implicit m: Manifest[T]): ValType[T] =
    new ValType[T]:
      val manifest = m

  /**
   * Force conversion
   * @param c
   * @return
   */
  def unsecure(c: Manifest[?]): ValType[Any] = apply(c.asInstanceOf[Manifest[Any]])

  def toNativeType(t: ValType[?]): ValType[?] =
    def native =
      val (contentType, level) = ValType.unArrayify(t)
      for 
        m ← classEquivalence(contentType.runtimeClass).map(_.manifest)
      yield 
        (0 until level).foldLeft(ValType.unsecure(m)) {
          (c, _) ⇒ c.toArray.asInstanceOf[ValType[Any]]
        }
    native getOrElse t

  def toTypeString(t: ValType[?], rootPrefix: Boolean = true, replaceObject$: Boolean = true): String =
    TypeTool.toString(rootPrefix = rootPrefix, replaceObject$ = replaceObject$)(toNativeType(t).manifest)

/**
 * Trait storing the type of prototypes, wrapping a [[scala.reflect.Manifest]]
 *  (types are not known before runtime)
 * @tparam T
 */
trait ValType[T] extends Id:
  def manifest: Manifest[T]
  override def toString = manifest.toString
  def id = manifest
  def runtimeClass = manifest.runtimeClass

object Val:

  /**
   * methods to convert a prototype to a prototype with type as array of the same type.
   *  toArray can be just a conversion of the type, but also go down recursively up to a specified level
   * @param prototype
   * @tparam T
   */
  implicit class ValToArrayDecorator[T](prototype: Val[T]) {
    def toArray(level: Int): Val[?] =
      def toArrayRecursive[A](prototype: Val[A], level: Int): Val[?] =
        if (level <= 0)
        then prototype
        else
          val arrayProto = Val.copyWithType(prototype, prototype.`type`.toArray)
          if (level <= 1) arrayProto
          else toArrayRecursive(arrayProto, level - 1)

      toArrayRecursive(prototype, level)

    def toArray: Val[Array[T]] = Val.copyWithType(prototype, prototype.`type`.toArray)

    def array(level: Int) = toArray(level)
    def array = toArray

    def unsecureFromArray = fromArray(prototype.asInstanceOf[Val[Array[T]]])
    def unsecureType = prototype.`type`.asInstanceOf[Manifest[Any]]
  }

  def fromArray[T](v: Val[Array[T]]) = copyWithType(v, `type` = v.`type`.fromArray)

  implicit class ValFromArrayDecorator[T](v: Val[Array[T]]) {
    def fromArray: Val[T] = Val.fromArray(v)
  }

  implicit class valDecorator[T](v: Val[T]) {
    def withName(name: String) = Val[T](name)(v.`type`)
  }

  implicit def valToArrayConverter[T](p: Val[T]): Val[Array[T]] = p.toArray

  def apply[T](name: String, namespace: Namespace = Namespace.empty)(implicit t: ValType[T]): Val[T] = {
    assert(t != null)
    new Val[T](name, t, namespace)
  }

  def apply[T](implicit t: ValType[T], name: sourcecode.Name): Val[T] = apply[T](name.value)

  implicit lazy val valOrderingOnName: Ordering[Val[?]] = new Ordering[Val[?]] {
    override def compare(left: Val[?], right: Val[?]) =
      left.name compare right.name
  }

  implicit def isTypeable[T: Manifest]: Typeable[Val[T]] =
    new Typeable[Val[T]] {
      override def cast(t: Any): Option[Val[T]] =
        t match {
          case v: Val[?] if v.`type`.manifest == manifest[T] ⇒ Some(v.asInstanceOf[Val[T]])
          case _ ⇒ None
        }
      override def castable(t: Any): Boolean = cast(t).isDefined
      override def describe = s"Val[${manifest[T].toString}]"
    }

  val caseBoolean = TypeCase[Val[Boolean]]
  val caseInt = TypeCase[Val[Int]]
  val caseLong = TypeCase[Val[Long]]
  val caseDouble = TypeCase[Val[Double]]
  val caseString = TypeCase[Val[String]]
  val caseFile = TypeCase[Val[java.io.File]]

  val caseArrayBoolean = TypeCase[Val[Array[Boolean]]]
  val caseArrayInt = TypeCase[Val[Array[Int]]]
  val caseArrayLong = TypeCase[Val[Array[Long]]]
  val caseArrayDouble = TypeCase[Val[Array[Double]]]
  val caseArrayString = TypeCase[Val[Array[String]]]

  val caseArrayArrayBoolean = TypeCase[Val[Array[Array[Boolean]]]]
  val caseArrayArrayInt = TypeCase[Val[Array[Array[Int]]]]
  val caseArrayArrayLong = TypeCase[Val[Array[Array[Long]]]]
  val caseArrayArrayDouble = TypeCase[Val[Array[Array[Double]]]]
  val caseArrayArrayString = TypeCase[Val[Array[Array[String]]]]

  def name(namespace: Namespace, simpleName: String) =
    if (namespace.isEmpty) simpleName
    else s"${namespace.toString}$$$simpleName"

  def parseName(name: String) =
    val parts = name.split('$')
    (Namespace(parts.dropRight(1).toSeq*), parts.reverse.head)

  def copy[T](v: Val[T])(
    name:      String    = v.simpleName,
    namespace: Namespace = v.namespace
  ) = new Val(name, v.`type`, namespace)

  def copyWithType[T](v: Val[?], `type`: ValType[T]) = new Val(v.simpleName, `type`, v.namespace)

  extension (v: Val[?])
    def quotedString = s"\"${v}\""


object Namespace {
  def empty = Namespace()

  implicit def fromSeqString(s: Seq[String]): Namespace = Namespace(s *)
}

case class Namespace(names: String*) {
  override def toString =
    if (names.isEmpty) ""
    else names.mkString("$")
  def isEmpty = names.isEmpty
  def prefix(s: String*) = Namespace(s ++ names *)
  def postfix(s: String*) = Namespace(names ++ s *)
}

/**
 *  A Val represents variables to which a value can be attributed.
 *  More precisely, this corresponds to a prototype in the sense of C language prototypes.
 *  In C, function prototypes are defined in headers and can be understood as interfaces that can be implemented.
 *  In model experiments, a given parameter will potentially take several values in different contexts, and in a functional programming fashion can thus be understood as a prototype in the previous sense.
 *
 *  The Val is in practice composed of a type and a name. It allows to specify typed data transfert in OpenMOLE.
 *
 * @param simpleName name of the prototype
 * @param `type` type given by a [[ValType]]
 * @param namespace
 * @tparam T the type of the prototype. Values associated to this prototype should always be a subtype of T.
 */
class Val[T](val simpleName: String, val `type`: ValType[T], val namespace: Namespace) extends Id:
  /**
   * Get the name of the prototype.
   *
   * @return the name of the prototype
   */
  def name: String = Val.name(namespace, simpleName)

  /**
   * Test if this prototype can be assigned from another prototype. This work
   * in the same way as java.lang.Class.isAssignableFrom.
   *
   * @param p the prototype to test
   * @return true if the prototype is assignable from the given prototype
   */
  def isAssignableFrom(p: Val[?]): Boolean = TypeTool.assignable(p.`type`.manifest, `type`.manifest)

  /**
   * Test if a given object can be accepted by the prototype
   * (compares runtime classes using [[TypeTool.classAssignable]])
   *
   * @param obj
   * @return
   */
  def accepts(obj: Any): Boolean =
    obj == null || classAssignable(obj.getClass, `type`.runtimeClass)

  /**
   * Create a new prototype with the given name and same type as the calling one
   * @param name
   * @return
   */
  def withName(name: String) = Val[T](name, namespace = namespace)(`type`)

  /**
   * Create a new prototype with given type and same name as the calling one
   * @tparam T
   * @return
   */
  def withType[T: ValType] = Val[T](simpleName, namespace = namespace)

  /**
   * Change the namespace of a prototype
   * @param namespace
   * @return
   */
  def withNamespace(namespace: Namespace) = Val[T](name, namespace = namespace)(`type`)

  /**
   * Extract the value of a prototype from a given context
   * @param context
   * @return
   */
  def from(context: ⇒ Context): T = context(this)

  /**
   * Unique id : name and type define a prototype
   *  - Q : why is namespace not included here - two protos from different namespace are the same objects (in equals and hashcode sense)
   * @return
   */
  override def id = (name, `type`)

  override def toString = s"$name: ${`type`.toString}"


