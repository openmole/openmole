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
import org.openmole.core.workflow.tools.ScalaWrappedCompilation

import scala.util.Random

object FilteredSampling {

  def apply(sampling: Sampling, filters: SamplingFilter*) =
    new FilteredSampling(sampling, filters: _*)

}

sealed class FilteredSampling(sampling: Sampling, filters: SamplingFilter*) extends Sampling {

  override def inputs = sampling.inputs
  override def prototypes = sampling.prototypes

  override def build(context: ⇒ Context)(implicit rng: Random): Iterator[Iterable[Variable[_]]] =
    sampling.build(context).filter(sample ⇒ !filters.exists(!_(Context(sample))))

}

object SamplingFilter {
  def apply(code: String) = new ScalaSamplingFilter(code)
  implicit def stringToFilter(code: String) = apply(code)
}

trait SamplingFilter {
  def apply(factorsValues: Context): Boolean
}

class ScalaSamplingFilter(code: String) extends SamplingFilter {
  @transient lazy val proxy = ScalaWrappedCompilation.raw(code)
  def apply(factorsValues: Context) = proxy.run(factorsValues).asInstanceOf[Boolean]
}
