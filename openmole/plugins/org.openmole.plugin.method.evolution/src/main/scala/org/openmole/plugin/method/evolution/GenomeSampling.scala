/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo.ga._
import fr.iscpif.mgo._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.core.model.data.DataModeMask
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.service.Random._
import org.openmole.misc.workspace.Workspace
import org.openmole.core.implementation.task.Task._

class GenomeSampling[GS](
    genome: IPrototype[GS],
    evolution: Evolution { type G = GS },
    size: Int,
    initialGenomes: Option[IPrototype[Array[Array[Double]]]] = None) extends Sampling {

  def prototypes = List(genome)
  override def inputs = super.inputs ++ initialGenomes.map { p ⇒ new Data(p, DataMode(DataModeMask.optional)) }

  def build(context: IContext) = {
    def toSamplingLine(g: GS) = List(new Variable(genome, g))

    val rng = newRNG(context.valueOrException(openMOLESeed))

    val genomes =
      initialGenomes.map {
        context.value(_) match {
          case Some(v) ⇒
            v.toSeq.map {
              g ⇒ toSamplingLine(evolution.factory(g))
            }.take(size)
          case None ⇒ Seq.empty
        }
      }.getOrElse(Seq.empty)

    (genomes ++
      (0 until size - genomes.size).map { i ⇒ toSamplingLine(evolution.factory.random(rng)) }).iterator
  }
}
