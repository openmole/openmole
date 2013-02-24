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

import org.openmole.core.model.data._
import org.openmole.core.model.sampling._

sealed class ZipWithIndexSampling(val reference: Sampling, val index: Prototype[Int]) extends Sampling {

  override def inputs = reference.inputs
  override def prototypes = index :: reference.prototypes.toList

  override def build(context: Context): Iterator[Iterable[Variable[_]]] =
    reference.build(context).zipWithIndex.map {
      case (line, i) ⇒ line ++ List(Variable(index, i))
    }

}
