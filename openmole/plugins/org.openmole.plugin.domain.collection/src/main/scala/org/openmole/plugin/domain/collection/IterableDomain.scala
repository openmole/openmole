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

package org.openmole.plugin.domain.collection

import org.openmole.core.model.data.IContext
import org.openmole.core.model.domain.IDomain

import scala.collection.JavaConversions._

class IterableDomain[T](iterable: Iterable[T]) extends IDomain[T] {

  def this(elements: T*) = this(elements)
    
  def this(iterable: java.lang.Iterable[_ <: T]) = this(asScalaIterable(iterable))
    
  override def iterator(global: IContext, context: IContext): Iterator[T] = iterable.iterator
}
