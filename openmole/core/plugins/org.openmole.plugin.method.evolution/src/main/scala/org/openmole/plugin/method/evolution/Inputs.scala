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

sealed trait Input {
  def min: String
  def max: String
  def prototype: Prototype[_]
  def size: Int
}

case class Scalar(prototype: Prototype[Double], min: String, max: String) extends Input {
  def size = 1
}

case class Sequence(prototype: Prototype[Array[Double]], min: String, max: String, size: Int) extends Input

case class Inputs(inputs: Seq[Input]) {
  def size: Int =
    Try(inputs.map(_.size).sum).getOrElse(0)
}
