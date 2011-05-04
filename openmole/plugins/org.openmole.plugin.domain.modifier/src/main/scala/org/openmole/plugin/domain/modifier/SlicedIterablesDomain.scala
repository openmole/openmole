/*
 * Copyright (C) 2011 romain
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

package org.openmole.plugin.domain.modifier

import org.openmole.core.model.data.IContext
import org.openmole.core.model.domain.IDomain
import collection.JavaConversions._

class SlicedIterableDomain[T](val domain: IDomain[T], val size: Int) extends IDomain[java.lang.Iterable[T]] {

  override def iterator(context: IContext): Iterator[java.lang.Iterable[T]] = 
    new Iterator[java.lang.Iterable[T]] {
      private val iterator = domain.iterator(context)
      private var nextSlice = toSlice
      
      private def toSlice = asJavaIterable(iterator.slice(0, size).toIterable)

      override def next = {
        val ret = toSlice
        nextSlice = toSlice
        ret
      }
      
      override def hasNext = !nextSlice.isEmpty
    }
  

}
