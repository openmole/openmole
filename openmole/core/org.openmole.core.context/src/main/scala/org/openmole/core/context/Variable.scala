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
import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workspace.Workspace
import org.openmole.tool.random
import shapeless.Typeable

import scala.reflect.ClassTag
import scala.util.Random

object Variable {
  def openMOLENameSpace = Namespace("openmole")

  /**
   * implicit conversion of a tuple (prototype,value) to a Variable
   * @param t
   * @tparam T
   * @return
   */
  implicit def tupleWithValToVariable[@specialized T](t: (Val[T], T)): Variable[T] = apply(t._1, t._2)

  /**
   * implicit conversion of tuple (prototype name, value)
   * @param t
   * @tparam T
   * @return
   */
  implicit def tupleToVariable[@specialized T: Manifest](t: (String, T)): Variable[T] = apply(Val[T](t._1), t._2)

  /**
   * Unsecure constructor, trying to cast the provided value to the type of the prototype
   * @param p prototype
   * @param v value
   * @tparam T
   * @return
   */
  def unsecure[@specialized T](p: Val[T], v: Any): Variable[T] = Variable[T](p, v.asInstanceOf[T])

  /**
   * Seed for rng
   */
  val openMOLESeed = Val[Long]("seed", namespace = openMOLENameSpace)

  def copy[@specialized T](v: Variable[T])(prototype: Val[T] = v.prototype, value: T = v.value): Variable[T] = apply(prototype, value)

  object ConstructArray {
    implicit def javaCollection = new ConstructArray[java.util.AbstractCollection[Any]] {
      def size(c: java.util.AbstractCollection[Any]) = c.size()
      def iterable(c: java.util.AbstractCollection[Any]) = c
    }
  }

  trait ConstructArray[T] {
    def size(t: T): Int
    def iterable(t: T): java.lang.Iterable[Any]
  }

  def constructArray[CA: Manifest](
    prototype:  Val[_],
    collection: CA,
    toValue:    (Any, Class[_]) ⇒ Any)(implicit construct: ConstructArray[CA]) = {

    val (multiArrayType, depth): (ValType[_], Int) = ValType.unArrayify(prototype.`type`)

    // recurse in the multi array
    def constructMultiDimensionalArray(
      collection:   CA,
      currentArray: AnyRef,
      arrayType:    Class[_],
      maxDepth:     Int,
      toValue:      (Any, Class[_]) ⇒ Any): Unit = {
      assert(maxDepth >= 1)
      val it = construct.iterable(collection).iterator()
      var i = 0
      while (it.hasNext) {
        val v = it.next
        if (maxDepth == 1) {
          try java.lang.reflect.Array.set(currentArray, i, toValue(v, arrayType))
          catch {
            case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when adding a variable of type ${v.getClass} in an array of type ${arrayType}")
          }
        }
        else {
          v match {
            case v: CA ⇒ constructMultiDimensionalArray(v, java.lang.reflect.Array.get(currentArray, i), arrayType, maxDepth - 1, toValue(_, _))
            case _     ⇒ throw new UserBadDataError(s"Error when recursing at depth ${maxDepth} in a multi array of type ${multiArrayType}, value ${v} is not an instance of class ${implicitly[Manifest[CA]]}")
          }
        }
        i = i + 1
      }
    }

    def extractDimensions(collection: CA, depth: Int) = {
      // recurse to get sizes, Nested LogoLists assumed rectangular : size of first element is taken for each dimension
      // will fail if the depth of the prototype is not the depth of the LogoList
      def extractDimensions0(collection: CA, dims: Seq[Int], maxDepth: Int): Seq[Int] = {
        assert(maxDepth >= 1)
        val size = construct.size(collection)
        if (maxDepth == 1) dims ++ Seq(size)
        else if (size == 0) extractDimensions0(collection, dims ++ Seq(0), maxDepth - 1)
        else {
          val v = construct.iterable(collection).iterator().next()
          v match {
            case v: CA ⇒ extractDimensions0(v, dims ++ Seq(size), maxDepth - 1)
            case _     ⇒ throw new UserBadDataError(s"Error when recursing at depth ${maxDepth} in a multi array of type ${multiArrayType}, value ${v} of type ${v.getClass} found expected ${manifest[CA]}")
          }
        }
      }

      try extractDimensions0(collection, Seq.empty, depth)
      catch {
        case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when mapping a prototype array of depth ${depth} and type ${multiArrayType} with nested LogoLists")
      }
    }

    val dimensions = extractDimensions(collection, depth)
    val array = java.lang.reflect.Array.newInstance(multiArrayType.runtimeClass.asInstanceOf[Class[_]], dimensions: _*)
    constructMultiDimensionalArray(collection, array, multiArrayType.runtimeClass.asInstanceOf[Class[_]], depth, toValue)

    Variable(prototype.asInstanceOf[Val[Any]], array)
  }

}

/**
 * A Variable is a prototype with a value
 * @param prototype the prototype
 * @param value the value
 * @tparam T type of the Variable
 */
case class Variable[@specialized T](prototype: Val[T], value: T) {
  override def toString: String = prettified(Int.MaxValue)
  def prettified(snipArray: Int) = prototype.name + "=" + (if (value != null) value.prettify(snipArray) else "null")
  def name = prototype.name
}

