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

package org.openmole.tool.types

import java.util

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import java.lang.reflect.{ Type ⇒ JType, Array ⇒ _, _ }
import scala.reflect.ClassTag
import scala.reflect.Manifest.{ classType, intersectionType, arrayType, wildcardType }

object TypeTool {

  implicit class ManifestDecoration[T](m: Manifest[T]) {
    def isArray = m.runtimeClass.isArray
    def toArray = m.arrayManifest
    def asArray = m.asInstanceOf[Manifest[Array[T]]]
    def toClassTag = ClassTag[T](m.runtimeClass)
    def array(ts: T*): Array[T] = {
      val a = m.newArray(ts.size)
      for { (t, i) <- ts.zipWithIndex } a(i) = t
      a
    }
  }

  def manifestFromArrayUnsecure(m: Manifest[Array[_]]) = m.typeArguments.head.asInstanceOf[Manifest[Any]]

  implicit class ManifestArrayDecoration[T](m: Manifest[Array[T]]) {
    def fromArray: Manifest[T] = m.typeArguments.head.asInstanceOf[Manifest[T]]
  }

  /*   implicit class TypeDecoration(t: TypeRepr) {
    def isArray = t <:< definitions.ArrayClass.toType
    def fromArray = t.typeArgs.head
    def toArray = appliedType(definitions.ArrayClass.toType, List(t))
  } */

  implicit class ClassTagDecoration[T](c: ClassTag[T]) {
    def isArray = c.runtimeClass.isArray
    def toArray = c.wrap
  }

  implicit class ArrayClassTagDecorator[T](c: ClassTag[Array[T]]) {
    def fromArray: ClassTag[T] = ClassTag(c.runtimeClass.getComponentType)
  }

  @tailrec private def unArrayify(m1: Manifest[_], m2: Manifest[_], level: Int = 0): (Manifest[_], Manifest[_], Int) = {
    if (!m1.isArray || !m2.isArray) (m1, m2, level)
    else unArrayify(m1.asArray.fromArray, m2.asArray.fromArray, level + 1)
  }

  case class NativeType[T](native: Class[_], java: Class[_], scala: Class[T])(implicit val manifest: Manifest[T])

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
    classEquivalences.find(_.java == c) orElse
      classEquivalences.find(_.scala == c)

  //def typeEquivalence(t: Type) = classEquivalences.find(_.typeTag.tpe == t)

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
        TypeTool.getClass.getClassLoader.loadClass(s)
      }
      catch {
        case e: ClassNotFoundException ⇒ throw new ClassNotFoundException("The class " + s + " has not been found", e)
      }
    }
  )

  def clazzOf(v: Any) = {
    v match {
      case null      ⇒ classOf[Null]
      case r: AnyRef ⇒ r.getClass
    }
  }

  def classAssignable(from: Class[_], to: Class[_]) = {
    def unArrayify(c1: Class[_], c2: Class[_]): (Class[_], Class[_]) =
      if (!c1.isArray || !c2.isArray) (c1, c2) else unArrayify(c1.getComponentType, c2.getComponentType)

    val (ufrom, uto) = unArrayify(from, to)
    val eqTo = classEquivalence(uto).map(_.native).getOrElse(to)
    val eqFrom = classEquivalence(ufrom).map(_.native).getOrElse(from)

    eqTo.isAssignableFrom(eqFrom)
  }

  def assignable(from: Manifest[_], to: Manifest[_]): Boolean =
    unArrayify(from, to) match {
      case (c1, c2, _) ⇒
        val eqFrom = classEquivalence(from.runtimeClass).map(_.manifest).getOrElse(from)
        val eqTo = classEquivalence(to.runtimeClass).map(_.manifest).getOrElse(to)
        eqTo.runtimeClass.isAssignableFrom(eqFrom.runtimeClass)
    }

  def callByName[U: ClassTag, T](o: AnyRef, name: String, args: Vector[Any]) = {
    val handle = implicitly[ClassTag[U]].runtimeClass.getDeclaredMethods.find(_.getName == name).get
    handle.setAccessible(true)
    handle.invoke(o, args.toArray.map(_.asInstanceOf[Object]): _*).asInstanceOf[T]
  }

  def call(c: AnyRef, methodName: String, argsTypes: Seq[Class[_]], args: Seq[AnyRef]) = {
    val m = c.getClass.getMethod(methodName, argsTypes: _*)
    m.setAccessible(true)
    m.invoke(c, args: _*)
  }

  def fillArray(m: Manifest[_], s: Seq[_]) = {
    val values = m.newArray(s.size)
    s.zipWithIndex.foreach { case (v, i) ⇒ java.lang.reflect.Array.set(values, i, v) }
    values
  }

  def toString[T](implicit manifest: Manifest[T]) = {
    val tpe =
      manifest.toString.
        replace(".package$", ".").
        replace("$", ".").trim
    val wildCard = "_ <: "
    if(tpe.startsWith(wildCard)) tpe.drop(wildCard.size) else tpe
  }

}

