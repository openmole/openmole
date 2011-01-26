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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.sampling.filter

import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.sampling.ISampling

class FiltredSampling(sampling: ISampling, filters: Array[IFilter]) extends ISampling {

  override def build(context: IContext): Iterable[Iterable[IVariable[_]]] = {
    val samples = sampling.build(context)
    for(sample <- samples; if({
          var filtred = false
          val filterIt = filters.iterator
          while(filterIt.hasNext && !filtred) filtred = filterIt.next.apply(sample)
          filtred
        })) yield sample 
  }
}
