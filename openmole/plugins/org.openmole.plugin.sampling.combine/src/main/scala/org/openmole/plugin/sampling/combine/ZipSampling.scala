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
import scala.collection.mutable.ListBuffer
import scala.util.Random

object ZipSampling {

  def apply(samplings: Sampling*) =
    new ZipSampling(samplings: _*)

}

sealed class ZipSampling(val samplings: Sampling*) extends Sampling {

  override def inputs = PrototypeSet(samplings.flatMap(_.inputs))
  override def prototypes = samplings.flatMap(_.prototypes)

  override def build(context: ⇒ Context)(implicit rng: RandomProvider): Iterator[Iterable[Variable[_]]] =
    samplings.headOption match {
      case Some(reference) ⇒
        /* Compute plans */
        val cachedSample = samplings.tail.map { _.build(context) }.toArray

        /* Compose plans */
        val factorValuesCollection = new ListBuffer[Iterable[Variable[_]]]

        val valuesIterator = reference.build(context)
        var oneFinished = false

        while (valuesIterator.hasNext && !oneFinished) {
          val values = new ListBuffer[Variable[_]]

          for (it ← cachedSample) {
            if (!it.hasNext) oneFinished = true
            else values ++= (it.next)
          }

          if (!oneFinished) {
            values ++= (valuesIterator.next)
            factorValuesCollection += values
          }
        }

        factorValuesCollection.iterator

      case None ⇒ Iterator.empty
    }

}
