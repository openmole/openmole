/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.tools.obj

import _root_.groovy.lang.GroovyShell
import org.openmole.core.exception.UserBadDataError
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import java.lang.reflect.{ Type ⇒ JType, Array ⇒ _, _ }
import scala.reflect.Manifest.{ classType, intersectionType, arrayType, wildcardType }
import scala.reflect.ClassManifest

object ClassUtils {

  implicit class ClassDecorator[T](c: Class[T]) {
    def equivalence = classEquivalence(c).asInstanceOf[Class[T]]

    def listSuperClasses = {
      new Iterator[Class[_]] {
        var cur: Class[_] = c

        override def hasNext = cur != null

        override def next: Class[_] = {
          val ret = cur
          cur = cur.getSuperclass
          ret
        }
      }.toIterable
    }

    def listSuperClassesAndInterfaces = listIndexedSuperClassesAndInterfaces.unzip._1

    def listIndexedSuperClassesAndInterfaces = {
      val toExplore = new ListBuffer[Class[_]]
      toExplore += c

      val ret = new ListBuffer[(Class[_], Int)]
      var index = 0

      while (!toExplore.isEmpty) {
        val current = toExplore.remove(0)
        ret += current -> index
        val superClass = current.getSuperclass
        if (superClass != null) toExplore += superClass
        for (inter ← current.getInterfaces) toExplore += inter
        index += 1
      }
      ret
    }

    def listImplementedInterfaces = {
      val toExplore = new ListBuffer[Class[_]]
      toExplore += c

      val ret = new ListBuffer[Class[_]]

      while (!toExplore.isEmpty) {
        val current = toExplore.remove(0)

        val superClass = current.getSuperclass
        if (superClass != null) toExplore += superClass

        for (inter ← current.getInterfaces) {
          toExplore += inter
          ret += inter
        }
      }

      ret
    }

    def fromArray = c.getComponentType

    def toManifest = classType[T](c)

  }

  @tailrec def unArrayify(m1: Class[_], m2: Class[_], level: Int = 0): (Class[_], Class[_], Int) = {
    if (!m1.isArray || !m2.isArray) (m1, m2, level)
    else unArrayify(m1.getComponentType, m2.getComponentType, level + 1)
  }

  def unArrayify(c: Iterable[Class[_]]): (Iterable[Class[_]], Int) = {
    @tailrec def rec(c: Iterable[Class[_]], level: Int = 0): (Iterable[Class[_]], Int) = {
      if (c.isEmpty || c.exists(!_.isArray)) (c, level)
      else rec(c.map {
        _.getComponentType
      }, level + 1)
    }
    rec(c)
  }

  def intersectionArray(t: Iterable[Class[_]]) =
    unArrayify(t) match {
      case (cls, level) ⇒
        val c = intersection(cls)
        def arrayManifest(m: Manifest[_], l: Int): Manifest[_] = if (l == 0) m else arrayManifest(m.arrayManifest, l - 1)
        arrayManifest(c, level)
    }

  def intersection(t: Iterable[Class[_]]) = {
    def intersectionClass(t1: Class[_], t2: Class[_]) = {
      val classes = (t1.listSuperClasses.toSet & t2.listSuperClasses.toSet)
      if (classes.isEmpty) classOf[Any]
      else classes.head
    }

    val c = t.reduceLeft((t1, t2) ⇒ intersectionClass(t1, t2))
    val interfaces = t.map(_.listImplementedInterfaces.toSet).reduceLeft((t1, t2) ⇒ t1 & t2)

    intersect((List(c) ++ interfaces))
  }

  def intersect(tps: Iterable[JType]): Manifest[_] = intersectionType(tps.toSeq map manifest: _*)

  def manifest(s: String): Manifest[_] = manifest(toClass(s))

  def manifest[T](cls: Class[T]): Manifest[T] = classType(cls)

  def manifest(tp: JType): Manifest[_] = tp match {
    case x: Class[_] ⇒ classType(x)
    case x: ParameterizedType ⇒
      val owner = x.getOwnerType
      val raw = x.getRawType() match {
        case clazz: Class[_] ⇒ clazz
      }
      val targs = x.getActualTypeArguments() map manifest

      (owner == null, targs.isEmpty) match {
        case (true, true)  ⇒ manifest(raw)
        case (true, false) ⇒ classType(raw, targs.head, targs.tail: _*)
        case (false, _)    ⇒ classType(manifest(owner), raw, targs: _*)
      }
    case x: GenericArrayType ⇒ arrayType(manifest(x.getGenericComponentType))
    case x: WildcardType     ⇒ wildcardType(intersect(x.getLowerBounds), intersect(x.getUpperBounds))
    case x: TypeVariable[_]  ⇒ intersect(x.getBounds())
  }

  case class NativeType[T](native: Class[_], java: Class[_], scala: Class[T])(implicit val scalaManifest: Manifest[T])

  lazy val classEquivalences = Seq(
    NativeType(java.lang.Byte.TYPE, classOf[java.lang.Byte], classOf[Byte]),
    NativeType(java.lang.Short.TYPE, classOf[java.lang.Short], classOf[Short]),
    NativeType(java.lang.Integer.TYPE, classOf[java.lang.Integer], classOf[Int]),
    NativeType(java.lang.Long.TYPE, classOf[java.lang.Long], classOf[Long]),
    NativeType(java.lang.Float.TYPE, classOf[java.lang.Float], classOf[Float]),
    NativeType(java.lang.Double.TYPE, classOf[java.lang.Double], classOf[Double]),
    NativeType(java.lang.Character.TYPE, classOf[java.lang.Character], classOf[Char]),
    NativeType(java.lang.Boolean.TYPE, classOf[java.lang.Boolean], classOf[Boolean])
  )

  def classEquivalence(c: Class[_]) =
    classEquivalences.find(_.java == c).map(_.native) orElse classEquivalences.find(_.scala == c).map(_.native) getOrElse (c)

  def toClass(s: String) = classEquivalence(
    s match {
      case "Byte"       ⇒ classOf[Byte]
      case "Short"      ⇒ classOf[Short]
      case "Int"        ⇒ classOf[Int]
      case "int"        ⇒ classOf[Int]
      case "Long"       ⇒ classOf[Long]
      case "long"       ⇒ classOf[Long]
      case "Float"      ⇒ classOf[Float]
      case "Double"     ⇒ classOf[Double]
      case "double"     ⇒ classOf[Double]
      case "Char"       ⇒ classOf[Char]
      case "Boolean"    ⇒ classOf[Boolean]
      case "String"     ⇒ classOf[String]
      case "File"       ⇒ classOf[java.io.File]
      case "BigInteger" ⇒ classOf[java.math.BigInteger]
      case "BigDecimal" ⇒ classOf[java.math.BigDecimal]
      case _ ⇒ try {
        classOf[GroovyShell].getClassLoader.loadClass(s)
      }
      catch {
        case e: ClassNotFoundException ⇒ throw new UserBadDataError(e, "The class " + s + " has not been found")
      }
    })

  def clazzOf(v: Any) = {
    v match {
      case null      ⇒ classOf[Null]
      case r: AnyRef ⇒ r.getClass
    }
  }

  implicit def manifestDecoration(m: Manifest[_]) = new {
    def isArray = m.runtimeClass.isArray
    def fromArray = m.runtimeClass.fromArray
  }

  implicit def manifestToClass[T](m: Manifest[T]) = m.runtimeClass

  implicit def classToManifestDecorator[T](c: Class[T]) = manifest(c)

  def assignable(from: Class[_], to: Class[_]) =
    unArrayify(from, to) match {
      case (c1, c2, _) ⇒ isAssignableFromPrimitive(c1, c2)
    }

  def isAssignableFromPrimitive(from: Class[_], to: Class[_]) = {
    val fromEq = classEquivalence(from)
    val toEq = classEquivalence(to)
    if (fromEq.isPrimitive || toEq.isPrimitive) fromEq == toEq
    else to.isAssignableFrom(from)
  }

}