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
import java.lang.reflect.{Type as JType, Array as _, *}
import javax.swing.JToolBar.Separator
import scala.reflect.ClassTag
import scala.reflect.Manifest.{arrayType, classType, intersectionType, wildcardType}

object TypeTool:

  implicit class ManifestDecoration[T](m: Manifest[T]):
    def isArray = m.runtimeClass.isArray
    def toArray = m.arrayManifest
    def asArray = m.asInstanceOf[Manifest[Array[T]]]
    def toClassTag = ClassTag[T](m.runtimeClass)
    def array(ts: T*): Array[T] =
      val a = m.newArray(ts.size)
      for (t, i) <- ts.zipWithIndex do a(i) = t
      a

  def manifestFromArrayUnsecure(m: Manifest[Array[?]]) = m.typeArguments.head.asInstanceOf[Manifest[Any]]

  extension [T](m: Manifest[Array[T]])
    def fromArray: Manifest[T] = m.typeArguments.head.asInstanceOf[Manifest[T]]

  /*   implicit class TypeDecoration(t: TypeRepr) {
    def isArray = t <:< definitions.ArrayClass.toType
    def fromArray = t.typeArgs.head
    def toArray = appliedType(definitions.ArrayClass.toType, List(t))
  } */

  extension [T](c: ClassTag[T])
    def isArray = c.runtimeClass.isArray
    def toArray = c.wrap

  extension [T](c: ClassTag[Array[T]])
    def fromArray: ClassTag[T] = ClassTag(c.runtimeClass.getComponentType)

  @tailrec private def unArrayify(m1: Manifest[?], m2: Manifest[?], level: Int = 0): (Manifest[?], Manifest[?], Int) = {
    if (!m1.isArray || !m2.isArray) (m1, m2, level)
    else unArrayify(m1.asArray.fromArray, m2.asArray.fromArray, level + 1)
  }

  case class NativeType[T](native: Class[?], java: Class[?], scala: Class[T])(implicit val manifest: Manifest[T])

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

  def classEquivalence(c: Class[?]) =
    classEquivalences.find(_.java == c) orElse
      classEquivalences.find(_.scala == c)

  //def typeEquivalence(t: Type) = classEquivalences.find(_.typeTag.tpe == t)

  def clazzOf(v: Any) =
    v match
      case null      => classOf[Null]
      case r: AnyRef => r.getClass

  def classAssignable(from: Class[?], to: Class[?]) =
    def unArrayify(c1: Class[?], c2: Class[?]): (Class[?], Class[?]) =
      if (!c1.isArray || !c2.isArray) (c1, c2) else unArrayify(c1.getComponentType, c2.getComponentType)

    val (ufrom, uto) = unArrayify(from, to)
    val eqTo = classEquivalence(uto).map(_.native).getOrElse(to)
    val eqFrom = classEquivalence(ufrom).map(_.native).getOrElse(from)

    eqTo.isAssignableFrom(eqFrom)

  def assignable(from: Manifest[?], to: Manifest[?]): Boolean =
    unArrayify(from, to) match
      case (c1, c2, _) =>
        val eqFrom = classEquivalence(from.runtimeClass).map(_.manifest).getOrElse(from)
        val eqTo = classEquivalence(to.runtimeClass).map(_.manifest).getOrElse(to)
        eqTo.runtimeClass.isAssignableFrom(eqFrom.runtimeClass)

  def callByName[U: ClassTag, T](o: AnyRef, name: String, args: Vector[Any]) = {
    val handle = implicitly[ClassTag[U]].runtimeClass.getDeclaredMethods.find(_.getName == name).get
    handle.setAccessible(true)
    handle.invoke(o, args.toArray.map(_.asInstanceOf[Object]) *).asInstanceOf[T]
  }

  def call(c: AnyRef, methodName: String, argsTypes: Seq[Class[?]], args: Seq[AnyRef]) = {
    val m = c.getClass.getMethod(methodName, argsTypes *)
    m.setAccessible(true)
    m.invoke(c, args *)
  }

  def fillArray(m: Manifest[?], s: Seq[_]) =
    val values = m.newArray(s.size)
    s.zipWithIndex.foreach { case (v, i) => java.lang.reflect.Array.set(values, i, v) }
    values

  def toString[T](replaceObject$: Boolean = true, rootPrefix: Boolean = true)(implicit manifest: Manifest[T]) =

    def manifestToString(m: Manifest[?]): String =
      val rc = m.runtimeClass
      if rc.isArray
      then s"Array[${manifestToString(m.typeArguments.head)}]"
      else
        if !rootPrefix ||
          rc.isPrimitive ||
          rc == classOf[Any] ||
          rc == classOf[AnyRef] ||
          rc == classOf[Nothing] ||
          rc.getPackageName.startsWith("java") ||
          rc.getPackageName.startsWith("javax")
        then m.toString
        else s"_root_.${m.toString()}"

    val tpe =
      if !replaceObject$
      then manifestToString(manifest)
      else
        manifestToString(manifest).
          replace(".package$", ".").
          replace("$", ".").trim

    val wildCard = "_ <: "

    if tpe.startsWith(wildCard)
    then tpe.drop(wildCard.size)
    else tpe

  def toManifest(s: String, classLoader: ClassLoader): Manifest[?] =
    def loadClass(c: String) = Class.forName(c, true, classLoader)

    def simpleType(st: String): Manifest[?] =
      st.trim match
        case "Any" => Manifest.Any
        case "AnyRef" => Manifest.AnyRef
        case "AnyVal" => Manifest.AnyVal
        case "Boolean" => Manifest.Boolean
        case "Byte" => Manifest.Byte
        case "Char" => Manifest.Char
        case "Double" => Manifest.Double
        case "Float" => Manifest.Float
        case "Int" => Manifest.Int
        case "Long" => Manifest.Long
        case "Nothing" => Manifest.Nothing
        case "Null" => Manifest.Null
        case "Object" => Manifest.Object
        case "Short" => Manifest.Short
        case "Unit" => Manifest.Unit
        case s => Manifest.classType(loadClass(s))

    def arrayType(t: Manifest[?]): Manifest[?] =
      Manifest.arrayType(t)

    def parametricType(t: String, parameters: Seq[Manifest[?]]) =
      parameters.headOption match
        case Some(h) => Manifest.classType(loadClass(t), h, parameters.tail *)
        case None => simpleType(t)

    case class ParsedType(main: String, parameter: Seq[String])

    def parseParametricType(t: String): ParsedType =
      val main = t.takeWhile(_ != '[')
      val parameters = t.drop(main.length).toArray

      def recurse(pos: Int, depth: Int, separators: List[Int]): List[Int] =
        if pos >= parameters.length
        then separators.reverse
        else
          parameters(pos) match
            case '[' => recurse(pos + 1, depth + 1, separators)
            case ']' => recurse(pos + 1, depth - 1, separators)
            case ',' if depth == 1 => recurse(pos + 1, depth, pos :: separators)
            case _ => recurse(pos + 1, depth, separators)

      def separators = recurse(0, 0, List())

      def split(string: Array[Char], separators: List[Int], chunks: List[String], dropped: Int): List[String] =
        separators.headOption match
          case None => (new String(string) :: chunks).reverse
          case Some(s) => split(string.drop(s - dropped + 1), separators.tail, new String(string.take(s - dropped)) :: chunks, dropped + s + 1)

      def chunks = split(parameters.drop(1).dropRight(1), separators.map(_ - 1), List(), 0)

      ParsedType(main, chunks)


    def parseType(t: String): Manifest[?] =
      val main = t.takeWhile(_ != '[')
      if main.length == t.length
      then simpleType(t)
      else
        val parsedType = parseParametricType(t)
        main.trim match
          case "Array" =>
            assert(parsedType.parameter.length == 1, parsedType.toString)
            arrayType(parseType(parsedType.parameter.head))
          case _ =>
            val parameters = parsedType.parameter.map(parseType)
            parametricType(main, parameters)

    parseType(s)
  end toManifest

  private lazy val primitiveTypes =
    Seq(
      classOf[Boolean],
      classOf[Byte],
      classOf[Char],
      classOf[Short],
      classOf[Int],
      classOf[Long],
      classOf[Float],
      classOf[Double],
      java.lang.Void.TYPE
    ).map(t => t.getName -> t).toMap

  def primitiveType(typeName: String): Option[Class[?]] = primitiveTypes.get(typeName)

