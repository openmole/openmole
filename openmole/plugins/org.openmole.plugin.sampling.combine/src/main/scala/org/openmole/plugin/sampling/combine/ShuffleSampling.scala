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
import org.openmole.tool.random._

object ShuffleSampling {

  implicit def isSampling[S]: IsSampling[ShuffleSampling[S]] = new IsSampling[ShuffleSampling[S]] {
    override def validate(s: ShuffleSampling[S]): Validate = s.sampling.validate(s.s)
    override def inputs(s: ShuffleSampling[S]): PrototypeSet = s.sampling.inputs(s.s)
    override def outputs(s: ShuffleSampling[S]): Iterable[Val[_]] = s.sampling.outputs(s.s)
    override def apply(s: ShuffleSampling[S]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
      import p._
      val array = s.sampling(s.s).from(context).toArray
      shuffle(array)(random())
      array.iterator
    }
  }

}

case class ShuffleSampling[S](s: S)(implicit val sampling: IsSampling[S])