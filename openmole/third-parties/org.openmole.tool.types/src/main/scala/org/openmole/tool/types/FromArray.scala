package org.openmole.tool.types

/*
 * Copyright (C) 2019 Romain Reuillon
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

object FromArray {

  implicit def iterableFromArray: FromArray[Iterable] = new FromArray[Iterable] {
    def apply[T](a: Array[T]) = a.toIterable
  }

  implicit def seqFromArray: FromArray[Seq] = new FromArray[Seq] {
    def apply[T](a: Array[T]) = a.toSeq
  }

  implicit def vectorFromArray: FromArray[Vector] = new FromArray[Vector] {
    def apply[T](a: Array[T]) = a.toVector
  }

  implicit def listFromArray: FromArray[List] = new FromArray[List] {
    def apply[T](a: Array[T]) = a.toList
  }

  implicit def arrayFromArray: FromArray[Array] = new FromArray[Array] {
    def apply[T](a: Array[T]) = a
  }

}

trait FromArray[A[_]] {
  def apply[T](a: Array[T]): A[T]
}
