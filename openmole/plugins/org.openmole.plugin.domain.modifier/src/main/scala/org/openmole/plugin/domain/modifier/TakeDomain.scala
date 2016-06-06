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

package org.openmole.plugin.domain.modifier

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.tools.FromContext

import scalaz._
import Scalaz._

object TakeDomain {

  implicit def isFinite[D, T] = new Finite[TakeDomain[D, T], T] with DomainInputs[TakeDomain[D, T]] {
    override def computeValues(domain: TakeDomain[D, T]) = domain.computeValues()
    override def inputs(domain: TakeDomain[D, T]): PrototypeSet = domain.inputs
  }

}

case class TakeDomain[D, +T](val domain: D, val size: FromContext[Int])(implicit discrete: Discrete[D, T], domainInputs: DomainInputs[D]) {
  def inputs = domainInputs.inputs(domain)
  def computeValues() =
    for {
      d ← discrete.iterator(domain)
      s ← size
    } yield d.slice(0, s).toIterable
}
