/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.implementation

import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.misc.tools.service._
import org.openmole.misc.workspace._
import scala.annotation.tailrec

package object data {

  import Context._

  implicit def tupleToParameter[T](t: (Prototype[T], T)) = Parameter(t._1, t._2)
  implicit def tuple3ToParameter[T](t: (Prototype[T], T, Boolean)) = Parameter(t._1, t._2, t._3)
  implicit def prototypeToData[T](p: Prototype[T]) = Data[T](p)
  implicit def dataIterableDecorator(data: Traversable[Data[_]]) = DataSet(data.toList)
  implicit def iterableOfPrototypeToIterableOfDataConverter(prototypes: Traversable[Prototype[_]]) = DataSet(prototypes.map { p ⇒ prototypeToData(p) })
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

  implicit def variableToContextConverter(variable: Variable[_]) = Context(variable)

  implicit def variablesToContextConverter(variables: Traversable[Variable[_]]): Context = variables.toContext

  implicit def prototypeDecorator[T](prototype: Prototype[T]) = new {
    def withName(name: String) = Prototype[T](name)(prototype.`type`)
  }

}