/*
 * Copyright (C) 07/05/13 Romain Reuillon
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

package org.openmole.plugin.sampling.hypothesis

import org.openmole.core.model.sampling._
import org.openmole.core.model.data._
import org.openmole.core.implementation.sampling.SamplingBuilder
import scala.collection.mutable.ListBuffer
import org.openmole.misc.exception.UserBadDataError

object HypothesisSampling {

  def apply(prototypes: Prototype[Any]*) = new SamplingBuilder {

    val cases = ListBuffer[List[List[Any]]]()

    def add(hs: List[List[Any]]) = {
      if (hs.size != prototypes.size) throw new UserBadDataError(s"The size of the exploration set is supposed to be ${prototypes.size} but a list ${hs.size} is provided.")
      for {
        (h, p) ← (hs zip prototypes)
        v ← h
        if (!p.accepts(v))
      } throw new UserBadDataError(s"Values $v incompatible with prototype $p")
      cases += hs
    }

    def toSampling = new HypothesisSampling(prototypes, cases.toList)
  }

}

sealed class HypothesisSampling(override val prototypes: Seq[Prototype[Any]], val cases: List[List[List[Any]]]) extends Sampling {

  override def build(context: Context): Iterator[Iterable[Variable[_]]] = {
    def buildVariables(values: List[Any]) =
      for {
        (v, p) ← (values zip prototypes)
      } yield Variable.unsecure(p, v)

    val variableCases = cases.map(_.map(buildVariables))
    variableCases.view.reduceOption { (a, b) ⇒ for (v1 ← a; v2 ← b) yield v1 ++ v2 }.getOrElse(Iterable.empty).toIterator
  }

}
