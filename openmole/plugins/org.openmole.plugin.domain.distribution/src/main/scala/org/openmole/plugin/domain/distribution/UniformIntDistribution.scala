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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.domain.distribution

import java.util.Random
import org.openmole.core.model.data.IContext
import org.openmole.misc.workspace.Workspace
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IIterable
import org.openmole.misc.tools.service.Random._
import org.openmole.core.implementation.task.Task._

sealed class UniformIntDistribution(max: Option[Int] = None) extends IDomain[Int] with IIterable[Int] {
 

  override def iterator(context: IContext): Iterator[Int] = {
    val rng = newRNG(context.valueOrException(openMOLESeed))
    Iterator.continually {
      max match {
        case Some(i) => rng.nextInt(i)
        case None => rng.nextInt
      }
    }
  }
}
