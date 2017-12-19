/*
 * Copyright (C) 27/01/14 Romain Reuillon
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

package org.openmole.plugin.method.evolution

import mgo.double2Scalable
import org.openmole.core.context._
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain.Bounds
import org.openmole.core.workflow.sampling.Factor

import scala.annotation.tailrec
import cats._
import cats.implicits._
import org.openmole.core.fileservice.FileService
import org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble
import org.openmole.core.workspace.NewFile
import org.openmole.tool.random.RandomProvider

object UniqueGenome {

  import FromContext.asMonad._
  def size(g: UniqueGenome) =
    g.inputs.toVector.map(_.size).foldM(0)((b, s) ⇒ s.map(_ + b))

  def apply(inputs: Genome): UniqueGenome = {
    val prototypes = inputs.map(_.prototype).distinct
    new UniqueGenome(prototypes.map(p ⇒ inputs.reverse.find(_.prototype == p).get))
  }

  implicit def genomeToSeqOfInput(g: UniqueGenome): Seq[ScalarOrSequenceOfDouble[_]] = g.inputs
}

class UniqueGenome(val inputs: Seq[ScalarOrSequenceOfDouble[_]]) extends AnyVal