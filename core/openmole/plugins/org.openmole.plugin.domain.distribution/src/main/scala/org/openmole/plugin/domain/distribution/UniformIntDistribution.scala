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

package org.openmole.plugin.domain.distribution

import java.util.Random
import org.openmole.core.model.data._
import org.openmole.misc.workspace._
import org.openmole.core.model.domain._
import org.openmole.misc.tools.service.Random._
import org.openmole.core.implementation.task.Task._

sealed class UniformIntDistribution(max: Option[Int] = None) extends Domain[Int] with Discrete[Int] {

  override def iterator(context: Context): Iterator[Int] = {
    val rng = newRNG(context(openMOLESeed))
    Iterator.continually {
      max match {
        case Some(i) ⇒ rng.nextInt(i)
        case None ⇒ rng.nextInt
      }
    }
  }
}
