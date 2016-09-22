/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.tool.random

object RandomProvider {

  def apply(_rng: ⇒ scala.util.Random) = new RandomProvider {
    override lazy val rng: scala.util.Random = _rng
  }

  def empty = RandomProvider(throw new RuntimeException("No random number generator is available"))
}

trait RandomProvider <: (() ⇒ scala.util.Random) {
  def rng: scala.util.Random
  def apply() = rng
}
