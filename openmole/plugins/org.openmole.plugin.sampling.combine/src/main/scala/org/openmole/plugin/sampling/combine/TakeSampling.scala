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

package org.openmole.plugin.sampling.combine

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object TakeSampling {

  implicit def isSampling[S]: IsSampling[TakeSampling[S]] = s ⇒ {
    def validate: Validate = s.sampling(s.s).validate ++ s.n.validate
    def inputs: PrototypeSet = s.sampling(s.s).inputs
    def outputs: Iterable[Val[?]] = s.sampling(s.s).outputs
    def apply: FromContext[Iterator[Iterable[Variable[?]]]] = FromContext { p ⇒
      import p._
      s.sampling(s.s).sampling.from(context).take(s.n.from(context))
    }

    Sampling(
      apply,
      outputs,
      inputs = inputs,
      validate = validate
    )
  }

}

case class TakeSampling[S](s: S, n: FromContext[Int])(implicit val sampling: IsSampling[S])
