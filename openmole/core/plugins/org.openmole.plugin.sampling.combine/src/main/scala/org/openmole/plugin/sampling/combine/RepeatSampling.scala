/*
 * Copyright (C) 2014 Romain Reuillon
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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.FromContext

import scala.util.Random

object RepeatSampling {

  def apply[T](sampling: Sampling, times: FromContext[Int]) = new RepeatSampling(sampling, times)

}

sealed class RepeatSampling(val sampling: Sampling, val times: FromContext[Int]) extends Sampling {

  override def inputs = sampling.inputs
  override def prototypes = sampling.prototypes.map(_.toArray)

  override def build(context: Context)(implicit rng: Random): Iterator[Iterable[Variable[_]]] = {
    def sampled =
      for {
        vs ← sampling.build(context).map(_.toSeq).toSeq.transpose
      } yield {
        val p = vs.head.prototype
        Variable.unsecure(p.toArray, vs.map(_.value).toArray(p.`type`.asInstanceOf[Manifest[Any]]))
      }

    Iterator.continually(sampled).take(times.from(context))
  }

}
