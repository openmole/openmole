/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.sampling.combine

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.plugin.domain.modifier._

import cats.implicits._

object ZipWithNameSampling {

  def apply[D, T: CanGetName](factor: Factor[D, T], name: Val[String])(implicit discrete: DiscreteFromContext[D, T]) =
    new ZipWithNameSampling(factor, name)
}

class ZipWithNameSampling[D, T: CanGetName](val factor: Factor[D, T], val name: Val[String])(implicit discrete: DiscreteFromContext[D, T]) extends Sampling {

  override def inputs = FactorSampling(factor).inputs
  override def prototypes = List(factor.value, name)

  override def apply() =
    for {
      d ← discrete.iterator(factor.domain)
    } yield d.map { v ⇒ List(Variable(factor.value, v), Variable(name, implicitly[CanGetName[T]].getName(v))) }

}
