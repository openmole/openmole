package org.openmole.plugin.sampling.combine

/*
 * Copyright (C) 2019 Romain Reuillon
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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object SubsetSampling {

  implicit def isSampling[S]: IsSampling[SubsetSampling[S]] = s ⇒ {
    def validate: Validate = s.sampling(s.s).validate
    def inputs: PrototypeSet = s.sampling(s.s).inputs
    def outputs: Iterable[Val[?]] = s.sampling(s.s).outputs
    def apply: FromContext[Iterator[Iterable[Variable[?]]]] = FromContext { p ⇒
      import p._
      val sizeValue = s.size.from(context)
      s.sampling(s.s).sampling.from(context).drop(s.n.from(context) * sizeValue).take(sizeValue)
    }

    Sampling(
      apply,
      outputs,
      inputs = inputs,
      validate = validate
    )
  }

}

case class SubsetSampling[S](s: S, n: FromContext[Int], size: FromContext[Int] = 100)(implicit val sampling: IsSampling[S])
