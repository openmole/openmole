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

trait IRangeConverter[A, B] extends IRange[B] {
  def convert(e: A): B
  def underlyingRange: IRange[A]
  
  override def computeValues(context: IContext): Iterable[B] = underlyingRange.computeValues(context).map{convert(_)}
  override def step(context: IContext): B = convert(underlyingRange.step(context))
  override def min(context: IContext): B = convert(underlyingRange.min(context))
  override def max(context: IContext): B = convert(underlyingRange.max(context))
  override def range(context: IContext): B = convert(underlyingRange.range(context))
  override def center(context: IContext): B = convert(underlyingRange.center(context))
}
