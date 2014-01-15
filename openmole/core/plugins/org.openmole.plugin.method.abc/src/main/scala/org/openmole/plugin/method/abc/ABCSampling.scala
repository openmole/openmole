/*
 * Copyright (C) 15/01/14 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.abc

import org.openmole.core.model.sampling._
import fr.irstea.scalabc._
import org.openmole.core.model.data._
import org.openmole.core.implementation.task._
import org.openmole.misc.tools.service.Random._

abstract class ABCSampling extends Sampling {

  val abc: ABC
  def state: Prototype[abc.STATE]
  def size: Int

  def prototypes: Seq[Prototype[Double]]
  override def inputs = DataSet(Data(state))

  override def build(context: Context) = {
    val rng = newRNG(context(Task.openMOLESeed))
    abc.sample(context(state), size)(rng).map {
      sampled ⇒
        (prototypes zip sampled).map {
          case (v, s) ⇒ Variable(v, s)
        }
    }.toIterator
  }

}
