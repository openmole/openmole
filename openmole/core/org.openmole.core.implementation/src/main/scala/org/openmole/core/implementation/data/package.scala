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

import org.openmole.core.implementation.puzzle.Puzzle
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.data.Context._
import org.openmole.core.model.data._
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.misc.tools.service.Random
import org.openmole.misc.workspace.Workspace
import scala.annotation.tailrec

package object data {

  import Context._

  implicit def tupleToParameter[T](t: (IPrototype[T], T)) = new Parameter(t._1, t._2)
  implicit def prototypeToData[T](p: IPrototype[T]) = new Data[T](p)
  implicit def dataIterableDecorator(data: Traversable[IData[_]]) = new DataSet(data.toList)
  //  implicit def iterableOfPrototypeToIterableOfDataConverter(prototypes: Traversable[IPrototype[_]]): Traversable[IData[_]] = DataSet(prototypes)
  implicit def prototypeToStringConverter(p: IPrototype[_]) = p.name

  implicit def prototypeToArrayDecorator[T](prototype: IPrototype[T]) = new {
    def toArray(level: Int): IPrototype[_] = {
      def toArrayRecursive[A](prototype: IPrototype[A], level: Int): IPrototype[_] = {
        if (level <= 0) prototype
        else {
          val arrayProto = new Prototype(prototype.name)(prototype.`type`.arrayManifest).asInstanceOf[IPrototype[Array[_]]]
          if (level <= 1) arrayProto
          else toArrayRecursive(arrayProto, level - 1)
        }
      }

      toArrayRecursive(prototype, level)
    }

    def toArray: IPrototype[Array[T]] =
      new Prototype(prototype.name)(prototype.`type`.arrayManifest).asInstanceOf[IPrototype[Array[T]]]

  }

  implicit def prototypeFromArrayDecorator[T](prototype: IPrototype[Array[T]]) = new {

    def fromArray: IPrototype[T] =
      (new Prototype(prototype.name)(prototype.`type`.fromArray.toManifest)).asInstanceOf[IPrototype[T]]

  }

  implicit def dataToArrayDecorator[T](data: IData[T]) = new {
    def toArray: IData[Array[T]] = new Data[Array[T]](data.prototype.toArray, data.mode)
  }

  implicit def variablesToContextConverter(variables: Traversable[IVariable[_]]): Context = Context(variables)

  val optional = DataModeMask.optional

}