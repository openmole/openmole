/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.sampling.combine

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.sampling._

object ZipWithIndexSampling {

  def apply(sampling: Sampling, index: Val[Int]) =
    new ZipWithIndexSampling(sampling, index)

}

sealed class ZipWithIndexSampling(val sampling: Sampling, val index: Val[Int]) extends Sampling {

  override def inputs = sampling.inputs
  override def prototypes = index :: sampling.prototypes.toList

  override def apply() = FromContext.apply { (context, rng) ⇒
    sampling().from(context)(rng).zipWithIndex.map {
      case (line, i) ⇒ line ++ List(Variable(index, i))
    }
  }

}
