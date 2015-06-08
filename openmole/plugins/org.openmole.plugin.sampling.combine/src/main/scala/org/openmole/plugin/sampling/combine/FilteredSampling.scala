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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools.{ Condition, ScalaWrappedCompilation }

import scala.util.Random

object FilteredSampling {

  def apply(sampling: Sampling, keep: Condition) =
    new FilteredSampling(sampling, keep)

}

sealed class FilteredSampling(sampling: Sampling, keep: Condition) extends Sampling {

  override def inputs = sampling.inputs
  override def prototypes = sampling.prototypes

  override def build(context: ⇒ Context)(implicit rng: RandomProvider): Iterator[Iterable[Variable[_]]] =
    sampling.build(context).filter(sample ⇒ keep.evaluate(context ++ sample))

}

