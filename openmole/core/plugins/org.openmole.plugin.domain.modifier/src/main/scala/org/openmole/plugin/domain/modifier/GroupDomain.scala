/*
 * Copyright (C) 2011 romain
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

package org.openmole.plugin.domain.modifier

import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import collection.JavaConversions._
import org.openmole.misc.tools.obj.ClassUtils._

object GroupDomain {

  def apply[T](domain: Domain[T] with Discrete[T], size: Int)(implicit m: Manifest[T]) =
    new GroupDomain(domain, size)

}

sealed class GroupDomain[T](val domain: Domain[T] with Discrete[T], val size: Int)(implicit m: Manifest[T]) extends Domain[Array[T]] with Discrete[Array[T]] {

  override def inputs = domain.inputs

  override def iterator(context: Context): Iterator[Array[T]] =
    domain.iterator(context).grouped(size).map {
      i ⇒ i.toArray
    }

}
