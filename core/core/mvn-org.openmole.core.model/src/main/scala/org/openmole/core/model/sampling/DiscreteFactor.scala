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

package org.openmole.core.model.sampling

import org.openmole.core.model.data._
import org.openmole.core.model.domain._

object DiscreteFactor {

  def apply[T, D <: Domain[T] with Discrete[T]](f: Factor[T, D]) =
    new DiscreteFactor[T, D] {
      val prototype = f.prototype
      val domain = f.domain
    }

}

trait DiscreteFactor[T, +D <: Domain[T] with Discrete[T]] extends Factor[T, D] with Sampling {

  override def prototypes = List(prototype)

  override def build(context: Context): Iterator[collection.Iterable[Variable[T]]] =
    domain.iterator(context).map { v ⇒ List(Variable(prototype, v)) }

}
