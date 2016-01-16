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

package org.openmole.plugin.domain.collection

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.tools.FromContext

object VariableDomain {
  implicit def isFinite[T] = new Finite[VariableDomain[T], T] with DomainInputs[VariableDomain[T]] {
    override def inputs(domain: VariableDomain[T]) = Seq(domain.variable)
    override def computeValues(domain: VariableDomain[T]) =
      FromContext((context, rng) ⇒ context(domain.variable))
  }

  def apply[A](variable: Prototype[Array[A]]) = new VariableDomain[A](variable)
}

sealed class VariableDomain[A](val variable: Prototype[Array[A]])
