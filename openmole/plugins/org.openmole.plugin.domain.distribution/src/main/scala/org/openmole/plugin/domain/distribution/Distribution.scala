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

package org.openmole.plugin.domain.distribution

import scala.util.Random


object Distribution:
  given Distribution[Int] with
    override def next(rng: Random): Int = rng.nextInt
    override def next(rng: Random, max: Int): Int = rng.nextInt(max)


  given Distribution[Long] with
    override def next(rng: Random): Long = rng.nextLong
    override def next(rng: Random, max: Long): Long =
      if (max <= 0) throw new IllegalArgumentException("max must be positive")
      var bits: Long = 0
      var value: Long = 0
      while
        bits = (rng.nextLong() << 1) >>> 1
        value = bits % max
        (bits - value + (max - 1) < 0L)
      do ()
      value


  given Distribution[Double] with
    override def next(rng: Random): Double = rng.nextDouble
    override def next(rng: Random, max: Double): Double = rng.nextDouble * max


trait Distribution[T]:
  def next(rng: Random): T
  def next(rng: Random, max: T): T



