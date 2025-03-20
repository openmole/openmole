package org.openmole.tool.collection

import scala.reflect.ClassTag

/*
 * Copyright (C) 2024 Romain Reuillon
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

object ArrayStack:
  def apply[T: ClassTag](capacity: Int, growthFactor: Int = 2) =
    new ArrayStack(Array.ofDim[T](capacity), growthFactor, capacity, capacity)

class ArrayStack[T: ClassTag](private var array: Array[T], growthFactor: Int, private var capacity: Int, initialCapacity: Int):
  private var currentIndex: Int = 0

  def add(t: T): Unit = synchronized:
    if currentIndex == capacity then growCapacity()
    array(currentIndex) = t
    currentIndex += 1

  def size: Int = synchronized(currentIndex)

  def pop(): T = synchronized:
    currentIndex -= 1
    array(currentIndex)

  def clear(): Unit = synchronized:
    currentIndex = 0
    array = Array.ofDim[T](initialCapacity)

  private def growCapacity(): Unit =
    val newCapacity = capacity * growthFactor
    val newArray = Array.ofDim[T](newCapacity)
    System.arraycopy(array, 0, newArray, 0, array.length)
    array = newArray
    capacity = newCapacity

