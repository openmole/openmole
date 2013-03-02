/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.data

object Data {

  implicit lazy val dataOrderingOnName = new Ordering[Data[_]] {
    override def compare(left: Data[_], right: Data[_]) =
      Prototype.prototypeOrderingOnName.compare(left.prototype, right.prototype)
  }

  def apply[T](p: Prototype[T], m: DataMode) = new Data[T] {
    val prototype = p
    val mode = m
  }

  def apply[T](prototype: Prototype[T], masks: DataModeMask*): Data[T] = apply(prototype, DataMode(masks: _*))

}

/**
 * It modelizes atomic elements of data-flows. Data is a typed data chunk with
 * meta-information. Tasks takes Data in input and produce Data in output.
 * Data travels through transitions and data channels.
 *
 */
trait Data[T] {

  /**
   * The mode is meta-information on the Data. It indicates the manner a task
   * uses it. For a list of modalties see {@see DataModeMask}.
   *
   * @return mode of the data
   */
  def mode: DataMode

  /**
   * Data chunks are named and typed. Get the prototype (type and name) of
   * this data chunk.
   *
   * @return the prototype of the Data
   */
  def prototype: Prototype[T]

  override def toString = "Data " + prototype + " with mode " + mode

}
