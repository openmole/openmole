/**
 * Created by Romain Reuillon on 08/04/16.
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
 *
 */
package org.openmole.tool

package object math {

  def scale(v: Double, min: Double, max: Double, boundaryMin: Double, boundaryMax: Double) = {
    val factor = (boundaryMax - boundaryMin) / (max - min)
    (factor * (v - min) + boundaryMin)
  }

  implicit class Double2Scalable(d: Double) {
    def scale(min: Double, max: Double, originalMin: Double = 0, originalMax: Double = 1) =
      math.scale(d, originalMin, originalMax, min, max)

    def normalize(min: Double, max: Double) = math.scale(d, min, max, 0, 1)

  }

}
