/*
 * Copyright (C) 27/01/14 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.evolution

import org.openmole.core.model.data.Prototype
import util.Try

sealed trait Input[T] {
  def min: T
  def max: T
  def prototype: Prototype[_]
  def size: Int
}

case class Scalar[T](prototype: Prototype[Double], min: T, max: T) extends Input[T] {
  def size = 1
}

case class Sequence[T](prototype: Prototype[Array[Double]], min: T, max: T, size: Int) extends Input[T]

case class Inputs[T](inputs: Seq[Input[T]]) {
  def size: Int = Try(inputs.map(_.size).sum).getOrElse(0)
}
