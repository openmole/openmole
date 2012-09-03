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

package org.openmole.plugin.domain.distribution

import java.util.Random
import org.openmole.core.model.data.Context
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IFinite
import org.openmole.core.model.domain.IIterable
import org.openmole.misc.workspace.Workspace

sealed class FiniteUniformIntDistribution(size: Int, max: Option[Int] = None) extends IDomain[Int] with IIterable[Int] with IFinite[Int] {

  @transient lazy val innerDomain = new UniformIntDistribution(max)

  override def computeValues(context: Context): Iterable[Int] = innerDomain.iterator(context).take(size).toIterable

}