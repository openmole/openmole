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

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo.termination.TimedTermination

import scala.concurrent.duration.Duration

object Timed {

  def apply(_duration: Duration) = new GATermination with TimedTermination {
    type G = Any
    type F = Any
    type P = Any
    type MF = Any
    val stateManifest: Manifest[STATE] = manifest[STATE]
    val duration = _duration.toMillis
  }

}
