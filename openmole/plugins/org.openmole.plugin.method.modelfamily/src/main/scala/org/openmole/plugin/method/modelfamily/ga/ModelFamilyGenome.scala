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

package org.openmole.plugin.method.modelfamily.ga

import fr.iscpif.mgo.genome._
import monocle._
import monocle.macros._

import scala.util.Random

object ModelFamilyGenome {
  case class Genome(
    modelId: Int,
    values: Seq[Double],
    sigma: Seq[Double],
    mutation: Option[Int] = None,
    crossover: Option[Int] = None)
}

trait ModelFamilyGenome <: ModelId with Sigma with GA with RandomGenome with DynamicApplication {

  type G = ModelFamilyGenome.Genome

  def models: Int
  def genomeSize: Int

  def modelId = Lenser[G](_.modelId)

  def rawValues = Lenser[G](_.values)
  def sigma = Lenser[G](_.sigma)

  def fromMutation = Lenser[G](_.mutation)
  def fromCrossover = Lenser[G](_.crossover)

  def randomGenome(implicit rng: Random) = {
    def rnd = Stream.continually(rng.nextDouble).take(genomeSize).toIndexedSeq
    ModelFamilyGenome.Genome(rng.nextInt(models), rnd, rnd)
  }
}
