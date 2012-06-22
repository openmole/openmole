/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.method.sensitivity

import math._

trait TotalOrderEffect {
  def computeSensitivity(a: Seq[Double], b: Seq[Double], c: Seq[Double]) = {
    val n = a.size

    val bxcAvg = (b zip c map { case (b, c) ⇒ b * c } sum) / n

    val axaAvg = (a map { a ⇒ a * a } sum) / n
    val f0 = (a sum) / n

    1 - (bxcAvg - pow(f0, 2)) / (axaAvg - math.pow(f0, 2))
  }
}
