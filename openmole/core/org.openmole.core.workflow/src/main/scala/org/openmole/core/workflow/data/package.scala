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

package data {

  trait DataPackage {

    implicit def prototypeToStringConverter(p: Prototype[_]) = p.name
    implicit def dataToStringConverter(d: Data[_]) = d.prototype.name

    implicit def prototypeToArrayDecorator[T](prototype: Prototype[T]) = new {
      def toArray(level: Int): Prototype[_] = {
        def toArrayRecursive[A](prototype: Prototype[A], level: Int): Prototype[_] = {
          if (level <= 0) prototype
          else {
            val arrayProto = Prototype(prototype.name)(prototype.`type`.arrayManifest).asInstanceOf[Prototype[Array[_]]]
            if (level <= 1) arrayProto
            else toArrayRecursive(arrayProto, level - 1)
          }
        }

        toArrayRecursive(prototype, level)
      }

      def toArray: Prototype[Array[T]] =
        Prototype(prototype.name)(prototype.`type`.arrayManifest).asInstanceOf[Prototype[Array[T]]]

      def unsecureType = prototype.`type`.asInstanceOf[Manifest[Any]]

    }

    implicit def prototypeFromArrayDecorator[T](prototype: Prototype[Array[T]]) = new {

      def fromArray: Prototype[T] =
        (Prototype(prototype.name)(prototype.`type`.fromArray.toManifest)).asInstanceOf[Prototype[T]]

    }

    implicit def dataToArrayDecorator[T](data: Data[T]) = new {
      def toArray: Data[Array[T]] = Data[Array[T]](data.prototype.toArray, data.mode)
    }

    implicit def decorateVariableIterable(variables: Traversable[Variable[_]]) = new {
      def toContext: Context = Context(variables)
    }

    implicit def prototypeDecorator[T](prototype: Prototype[T]) = new {
      def withName(name: String) = Prototype[T](name)(prototype.`type`)
    }
  }
}

package object data extends DataPackage
