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

package org.openmole.plugin.domain.range

import org.openmole.core.model.data.IContext
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IIterable

sealed class InfinitePersistentCounter(counter: Iterator[Long]) extends IDomain[Long] with IIterable[Long] {

  def this(start: Long, step: Long) = {
    this(new Iterator[Long] {

      var value = start

      override def hasNext: Boolean = true

      override def next: Long = {
        val ret = value;
        value += step;
        ret
      }
    })
  }

  def this() = this(0L, 1L)

  override def iterator(context: IContext): Iterator[Long] = counter
}
