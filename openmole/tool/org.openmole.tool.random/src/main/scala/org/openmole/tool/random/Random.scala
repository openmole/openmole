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

package org.openmole.tool.random

import java.util.UUID

import org.apache.commons.math3.random.{ RandomAdaptor, RandomGenerator, Well44497b }

object Random { random =>
  def uuid2long(uuid: UUID) = uuid.getMostSignificantBits ^ uuid.getLeastSignificantBits

  def apply(seed: Long) = new SynchronizedRandom(new Well44497b(seed))

  @deprecated("6.0", "use apply(seed)")
  def newRNG(seed: Long) = apply(seed)

  def newUnsychronizedRNG(seed: Long) = new RandomAdaptor(new Well44497b(seed))

}

