/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.core.workflow

import org.openmole.core.macros.ExtractValName
import org.openmole.core.tools.obj.ClassUtils

import scala.language.experimental.macros
import reflect.macros.blackbox.{ Context ⇒ MContext }
import ClassUtils._

import scala.reflect.runtime.universe._

package data {

  trait DataPackage {

    implicit def prototypeToStringConverter(p: Prototype[_]) = p.name

    implicit class PrototypeTypeDecorator[T](p: PrototypeType[T]) {
      def toArray = PrototypeType[Array[T]](p.manifest.toArray)
      def array = toArray
      def isArray = p.manifest.isArray
      def asArray = p.asInstanceOf[PrototypeType[Array[T]]]
    }

    implicit class PrototypeTypeArrayDecorator[T](p: PrototypeType[Array[T]]) {
      def fromArray = PrototypeType[T](p.manifest.fromArray)
    }

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

    implicit def decorateVariableIterable(variables: Traversable[Variable[_]]) = new {
      def toContext: Context = Context(variables)
    }

    implicit def prototypeDecorator[T](prototype: Prototype[T]) = new {
      def withName(name: String) = Prototype[T](name)(prototype.`type`)
    }

    implicit def buildPrototypeType[T: Manifest]: PrototypeType[T] = PrototypeType[T]

    type Val[T] = Prototype[T]
  }

}

package object data extends DataPackage
