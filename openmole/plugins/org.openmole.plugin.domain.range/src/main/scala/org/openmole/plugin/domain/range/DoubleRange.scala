/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.plugin.domain.range


import java.lang.Double

class DoubleRange(val min: String, val max: String,val step: String) extends IRange[Double] with IRangeConverter[scala.Double, Double] {

  @transient lazy val underlyingRange = new ScalaDoubleRange(min, max, step)
  def convert(a: scala.Double): Double = a
  
  def this(min: String, max: String) = this(min, max, "1.0")
}