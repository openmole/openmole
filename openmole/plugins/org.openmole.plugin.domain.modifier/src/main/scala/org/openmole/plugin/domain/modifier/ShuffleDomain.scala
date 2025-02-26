/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.plugin.domain.modifier

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.domain
import org.openmole.tool.random._

object ShuffleDomain {

  implicit def isDiscrete[D, T]: DiscreteFromContextDomain[ShuffleDomain[D, T], T] = domain =>
    Domain(
      domain.iterator,
      domain.inputs,
      domain.validate
    )
}

case class ShuffleDomain[D, +T](domain: D)(implicit discrete: DiscreteFromContextDomain[D, T]) {
  def iterator = FromContext { p =>
    import p._
    discrete(domain).domain.from(context).toSeq.shuffled(random()).iterator
  }

  def inputs = discrete(domain).inputs
  def validate = discrete(domain).validate
}