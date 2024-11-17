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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.domain.DiscreteFromContextDomain
import org.openmole.plugin.domain.modifier.CanGetName

object ZipWithNameSampling {

  implicit def isSampling[D, T]: IsSampling[ZipWithNameSampling[D, T]] = s ⇒ {
    def validate: Validate = s.discrete(s.factor.domain).validate
    def inputs: PrototypeSet = Seq(s.factor.value)
    def outputs: Iterable[Val[?]] = List(s.factor.value, s.name)
    def apply: FromContext[Iterator[Iterable[Variable[?]]]] = FromContext { p ⇒
      import p._

      for {
        v ← s.discrete(s.factor.domain).domain.from(context)
      } yield List(Variable(s.factor.value, v), Variable(s.name, s.getName(v)))
    }

    Sampling(
      apply,
      outputs,
      inputs = inputs,
      validate = validate
    )
  }

}

case class ZipWithNameSampling[D, T](factor: Factor[D, T], name: Val[String])(implicit val discrete: DiscreteFromContextDomain[D, T], val getName: CanGetName[T])