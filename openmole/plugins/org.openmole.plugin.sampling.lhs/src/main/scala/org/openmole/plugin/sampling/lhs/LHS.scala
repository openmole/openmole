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

package org.openmole.plugin.sampling.lhs

import org.openmole.core.tools.script.GroovyProxyPool
import org.openmole.core.tools.service.Scaling._
import org.openmole.core.tools.service.Random._
import org.openmole.core.workflow.task.Task
import org.openmole.core.workflow.tools.FromContext
import util.Random
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import Task._
import org.openmole.core.workflow.tools._

object LHS {

  def apply(samples: FromContext[Int], factors: Factor[Double, Domain[Double] with Bounds[Double]]*) =
    new LHS(samples, factors: _*)

}

sealed class LHS(val samples: FromContext[Int], val factors: Factor[Double, Domain[Double] with Bounds[Double]]*) extends Sampling {

  override def inputs = DataSet(factors.flatMap(_.inputs))
  override def prototypes = factors.map { _.prototype }

  override def build(context: Context)(implicit rng: Random): Iterator[Iterable[Variable[Double]]] = {
    val s = samples.from(context)
    factors.map {
      f ⇒
        (0 until s).shuffled(rng).map {
          i ⇒ Variable(f.prototype, ((i + rng.nextDouble) / s).scale(f.domain.min(context), f.domain.max(context)))
        }
    }.transpose.toIterator

  }
}
