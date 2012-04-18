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

package org.openmole.plugin.sampling.filter

import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.sampling.ISampling
import org.openmole.core.implementation.data.Context

sealed class FiltredSampling(sampling: ISampling, filters: IFilter*) extends ISampling {

  override def inputs = sampling.inputs
  override def prototypes = sampling.prototypes
  
  override def build(context: IContext): Iterator[Iterable[IVariable[_]]] = 
    sampling.build(context).filter(sample => !filters.exists(!_(Context(sample))))

}
