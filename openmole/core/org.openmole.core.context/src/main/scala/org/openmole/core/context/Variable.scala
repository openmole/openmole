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

import org.openmole.core.context.ValType.unsecureFromArray
import org.openmole.core.exception.*
import org.openmole.core.workspace.Workspace
import org.openmole.tool.logger.Prettifier
import org.openmole.tool.random
import shapeless3.typeable.Typeable

import java.util
import scala.reflect.ClassTag
import scala.util.Random

object Variable:
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
  implicit def tupleToVariable[@specialized T: Manifest: izumi.reflect.Tag](t: (String, T)): Variable[T] = apply(Val[T](t._1), t._2)

  /**
   * Unsecure constructor, trying to cast the provided value to the type of the prototype
   * @param p prototype
   * @param v value
   * @tparam T
   * @return
   */
  def unsecure[@specialized T](p: Val[T], v: Any): Variable[T] = Variable[T](p, v.asInstanceOf[T])
  def unsecureUntyped(p: Val[?], v: Any): Variable[?] = Variable(p.asInstanceOf[Val[Any]], v)

  /**
   * Seed for rng
   */
  val openMOLESeed = Val[Long]("seed", namespace = Namespace.openmole)
  val openMOLEExperiment = Val[Long]("experiment", namespace = Namespace.openmole)

  def copy[@specialized T](v: Variable[T])(prototype: Val[T] = v.prototype, value: T = v.value): Variable[T] = apply(prototype, value)

  def constructArray(
    prototype:  Val[?],
    collection: Iterable[?],
    toValue:    (Any, Class[?]) => Any) =
    import scala.jdk.CollectionConverters.*

    val (multiArrayType, depth): (ValType[?], Int) = ValType.unArrayify(prototype.`type`)

    def isRectangular: Option[Seq[Int]] =
      val dimensions = Array.fill[Option[Int]](depth)(None)

      def testDimension(currentDepth: Int, size: => Int): Boolean =
        dimensions(currentDepth) match
          case None =>
            dimensions(currentDepth) = Some(size)
            true
          case Some(d) if d != size => false
          case _ => true

      def isRectangular0(c: Iterable[?], currentDepth: Int): Boolean =
        if !testDimension(currentDepth, c.size) then return false

        if currentDepth >= depth - 1
        then true
        else
          if c.isEmpty
          then testDimension(currentDepth + 1, size = 0)
          else
            c.forall:
              case e: Iterable[?] => isRectangular0(e, currentDepth + 1)
              case a: Array[?] => isRectangular0(a.toSeq, currentDepth + 1)
              case e => false

      if isRectangular0(collection, 0)
      then Some(dimensions.map(_.get).toSeq)
      else None


    def constructRectangularArray(dimensions: Seq[Int]) =
      // recurse in the multi array
      def constructMultiDimensionalArray(
        iterable:   Iterable[Any],
        currentArray: AnyRef,
        maxDepth:     Int,
        toValue:      (Any, Class[?]) => Any,
        valType:      ValType[?]): Unit =
        assert(maxDepth >= 1)

        def fillArray =
          val innerValType = ValType.unsecureFromArray(valType)
          iterable.iterator.zipWithIndex.foreach: (v, i) =>
            val innerValue = toValue(v, innerValType.runtimeClass)
            try java.lang.reflect.Array.set(currentArray, i, innerValue)
            catch
              case e: Throwable =>
                throw new UserBadDataError(e, s"Error when adding a value ${innerValue} of type ${innerValue.getClass} in an array of type ${innerValType.runtimeClass}")

        def recurse =
          val innerType = ValType.unsecureFromArray(valType)
          iterable.iterator.zipWithIndex.foreach: (v, i) =>
            v match
              case v: Iterable[Any] => constructMultiDimensionalArray(v, java.lang.reflect.Array.get(currentArray, i), maxDepth - 1, toValue, innerType)
              case v: Array[?] => constructMultiDimensionalArray(v.toIterable, java.lang.reflect.Array.get(currentArray, i), maxDepth - 1, toValue, innerType)
              case _ => throw new UserBadDataError(s"Error when recursing at depth ${maxDepth} in a multi array of type ${multiArrayType}, value ${v}") // is not an instance of class ${implicitly[Manifest[CA2]]}")

        if maxDepth == 1 then fillArray else recurse

//      def extractDimensions(collection: CA, depth: Int) =
//        // recurse to get sizes, Nested collections assumed rectangular : size of first element is taken for each dimension
//        def extractDimensions0(collection: CA, dims: Seq[Int], maxDepth: Int): Seq[Int] =
//          assert(maxDepth >= 1)
//          val size = construct.size(collection)
//          if maxDepth == 1
//          then dims ++ Seq(size)
//          else
//            if size == 0
//            then extractDimensions0(collection, dims ++ Seq(0), maxDepth - 1)
//            else
//              val v = construct.iterable(collection).iterator().next()
//              v match
//                case v: CA => extractDimensions0(v, dims ++ Seq(size), maxDepth - 1)
//                case _     => throw new UserBadDataError(s"Error when recursing at depth ${maxDepth} in a multi array of type ${multiArrayType}, value ${v} of type ${v.getClass} found expected ${manifest[CA]}")
//
//        try extractDimensions0(collection, Seq.empty, depth)
//        catch
//          case e: Throwable => throw new UserBadDataError(e, s"Error when mapping a prototype array of depth ${depth} and type ${multiArrayType} with nested LogoLists")
//
//      val dimensions = extractDimensions(collection, depth)
      val array = java.lang.reflect.Array.newInstance(multiArrayType.runtimeClass.asInstanceOf[Class[?]], dimensions*)
      constructMultiDimensionalArray(collection, array, depth, toValue, prototype.`type`)
      array

    def constructJaggedArray: Any =
      val (multiArrayType, totalDepth): (ValType[?], Int) = ValType.unArrayify(prototype.`type`)

      def constructMultiDimensionalArray(
        value: Any,
        valType: ValType[?],
        toValue: (Any, Class[?]) => Any,
        depth: Int): Any =

        if depth > 0
        then
          if valType.isArray
          then
            val fromArrayValType = ValType.fromArrayUnsecure(valType.asInstanceOf[ValType[Array[?]]])
            value match
              case collection: Iterable[Any] =>
                val fromArrayValType = ValType.fromArrayUnsecure(valType.asInstanceOf[ValType[Array[?]]])
                collection.map: e =>
                  constructMultiDimensionalArray(e, fromArrayValType, toValue, depth - 1)
                .toArray(using fromArrayValType.manifest)
              case v: Array[?] =>
                val fromArrayValType = ValType.fromArrayUnsecure(valType.asInstanceOf[ValType[Array[?]]])
                v.map: e =>
                  constructMultiDimensionalArray(e, fromArrayValType, toValue, depth - 1)
                .toArray(using fromArrayValType.manifest)
              case v => throw UserBadDataError(s"The variable $prototype expects nested collections (more array dimensions) but found element of type ${v.getClass}; the collection is not deeply nested enough to match the prototype")
          else throw UserBadDataError(s"The variable $prototype has fewer array dimensions than the provided collection; the collection is more deeply nested than the variable's type")
        else toValue(value, multiArrayType.runtimeClass.asInstanceOf[Class[?]])

      constructMultiDimensionalArray(collection, prototype.`type`, toValue, totalDepth)

    val array =
      isRectangular match
        case Some(dimensions) => constructRectangularArray(dimensions)
        case None => constructJaggedArray

    Variable(prototype.asInstanceOf[Val[Any]], array)


/**
 * A Variable is a prototype with a value
 * @param prototype the prototype
 * @param value the value
 * @tparam T type of the Variable
 */
case class Variable[@specialized T](prototype: Val[T], value: T):
  override def toString: String = prettified(Int.MaxValue)
  def prettified(snipArray: Int): String = s"${prototype.name}=${Prettifier.prettify(value, snipArray)}"
  def name = prototype.name

