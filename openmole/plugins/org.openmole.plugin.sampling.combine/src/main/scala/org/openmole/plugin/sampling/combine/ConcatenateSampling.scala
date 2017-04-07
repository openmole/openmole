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
package org.openmole.plugin.sampling.combine

import org.openmole.core.context.{ Val, PrototypeSet }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.sampling._

object ConcatenateSampling {
  def apply(samplings: Sampling*) = new ConcatenateSampling(samplings: _*)
}

class ConcatenateSampling(val samplings: Sampling*) extends Sampling {

  override lazy val inputs = PrototypeSet.empty ++ samplings.flatMap { _.inputs }

  override def prototypes: Iterable[Val[_]] = samplings.head.prototypes

  override def apply() = FromContext { p ⇒
    import p._
    samplings.toIterator.flatMap(_().from(context))
  }

}
