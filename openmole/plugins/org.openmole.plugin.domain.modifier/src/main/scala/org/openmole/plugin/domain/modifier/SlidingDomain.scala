/*
 * Copyright (C) 19/12/12 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.domain.modifier

import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.FromContext

import scalaz._
import Scalaz._

object SlidingDomain {

  implicit def isDiscrete[T, D] = new Discrete[Array[T], SlidingDomain[T, D]] {
    override def iterator(domain: SlidingDomain[T, D]) = domain.iterator()
    override def inputs(domain: SlidingDomain[T, D]) = domain.inputs
  }

  def apply[T: Manifest, D](domain: D, size: FromContext[Int], step: FromContext[Int] = 1)(implicit discrete: Discrete[T, D]) =
    new SlidingDomain[T, D](domain, size, step)

}

class SlidingDomain[T: Manifest, D](val domain: D, val size: FromContext[Int], val step: FromContext[Int] = 1)(implicit discrete: Discrete[T, D]) {

  def inputs = discrete.inputs(domain)

  def iterator() =
    for {
      it ← discrete.iterator(domain)
      si ← size
      st ← step
    } yield it.sliding(si, st).map(_.toArray)

}
