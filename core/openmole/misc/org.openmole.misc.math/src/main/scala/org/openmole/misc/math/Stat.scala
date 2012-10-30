/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.misc.math

object Stat {

  def median(serie: Iterable[Double]): Double = {
    val sortedSerie = serie.toArray.sorted
    val size = sortedSerie.size
    if (size % 2 == 0) (sortedSerie(size / 2) + sortedSerie((size / 2) - 1)) / 2 else sortedSerie((size / 2))
  }

  def medianAbsoluteDeviation(serie: Iterable[Double]): Double = {
    val m = median(serie)
    median(serie.map { v ⇒ math.abs(v - m) })
  }

  def average(serie: Iterable[Double]) = serie.sum / serie.size

  def meanSquareError(serie: Iterable[Double]) = {
    val avg = average(serie)
    average(serie.map { v ⇒ math.pow(v - avg, 2) })
  }
}
