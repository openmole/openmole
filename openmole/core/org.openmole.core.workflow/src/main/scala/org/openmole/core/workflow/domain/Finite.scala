/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.workflow.domain

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.FromContext

import scala.annotation.implicitNotFound
import scalaz.Scalaz._

object Finite {

  implicit def fromStatic[T, D](staticFinite: StaticFinite[T, D]) = new Finite[T, D] {
    override def computeValues(domain: D): FromContext[Iterable[T]] = staticFinite.computeValues(domain)
  }

}

@implicitNotFound("${D} is not a finite variation domain of type ${T}")
trait Finite[+T, -D] extends Domain[T, D] with Discrete[T, D] {
  def computeValues(domain: D): FromContext[collection.Iterable[T]]
  override def iterator(domain: D) = computeValues(domain).map(_.iterator)
}

@implicitNotFound("${D} is not a static finite variation domain of type ${T}")
trait StaticFinite[+T, -D] extends Domain[T, D] with StaticDiscrete[T, D] {
  def computeValues(domain: D): collection.Iterable[T]
  override def iterator(domain: D) = computeValues(domain).iterator
}