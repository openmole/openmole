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

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.sampling._

import org.openmole.core.tools.obj.ClassUtils._
import scala.util.Random

object ConcatenateSampling {
  def apply(samplings: Sampling*) = new ConcatenateSampling(samplings: _*)
}

class ConcatenateSampling(val samplings: Sampling*) extends Sampling {

  override lazy val inputs = PrototypeSet.empty ++ samplings.flatMap { _.inputs }

  override def prototypes: Iterable[Prototype[_]] = samplings.head.prototypes

  override def build(context: ⇒ Context)(implicit rng: RandomProvider): Iterator[Iterable[Variable[_]]] =
    samplings.toIterator.flatMap(_.build(context))

}
