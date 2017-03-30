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

import org.openmole.core.context.{ Context, Val, PrototypeSet, Variable }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.sampling._

object CompleteSampling {
  def apply(samplings: Sampling*) = new CompleteSampling(samplings: _*)
}

class CompleteSampling(val samplings: Sampling*) extends Sampling {

  override def inputs = PrototypeSet.empty ++ samplings.flatMap { _.inputs }
  override def prototypes: Iterable[Val[_]] = samplings.flatMap { _.prototypes }

  override def apply() = FromContext { p ⇒
    import p._
    if (samplings.isEmpty) Iterator.empty
    else
      samplings.tail.foldLeft(samplings.head().from(context)) {
        (a, b) ⇒ combine(a, b).from(context)
      }
  }

  def combine(s1: Iterator[Iterable[Variable[_]]], s2: Sampling) = FromContext { p ⇒
    import p._
    for (x ← s1; y ← s2().from(context ++ x)) yield x ++ y
  }

}
