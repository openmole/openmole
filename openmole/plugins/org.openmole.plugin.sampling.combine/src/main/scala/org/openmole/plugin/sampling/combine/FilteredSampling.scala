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

package org.openmole.plugin.sampling.combine

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object FilteredSampling {

  implicit def isSampling[S]: IsSampling[FilteredSampling[S]] = new IsSampling[FilteredSampling[S]] {
    override def validate(s: FilteredSampling[S]): Validate = s.sampling.validate(s.s) ++ s.keep.validate
    override def inputs(s: FilteredSampling[S]): PrototypeSet = s.sampling.inputs(s.s)
    override def outputs(s: FilteredSampling[S]): Iterable[Val[_]] = s.sampling.outputs(s.s)
    override def apply(s: FilteredSampling[S]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
      import p._
      s.sampling(s.s).from(context).filter(sample ⇒ s.keep.from(context ++ sample))
    }
  }

}

case class FilteredSampling[S](s: S, keep: Condition)(implicit val sampling: IsSampling[S])

