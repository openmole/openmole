/*
 * Copyright (C) 22/11/12 Romain Reuillon
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

package org.openmole.plugin.method

import org.openmole.core.model.data.Prototype
import fr.iscpif.mgo.Individual
import evolution.algorithm.{ GA ⇒ OMGA }

package object evolution {
  val GA = algorithm.GA

  implicit def seqOfTuplesToInputsConversion(s: Seq[(Prototype[Double], (String, String))]) =
    Inputs(s.map { case (p, (min, max)) ⇒ Scalar(p, min, max) })

  implicit def seqToInputsConversion(s: Seq[Input]) = Inputs(s)

  trait GAPuzzle[ALG <: GA.GAAlgorithm] {
    val evolution: OMGA[ALG]

    def archive: Prototype[evolution.A]
    def genome: Prototype[evolution.G]
    def individual: Prototype[Individual[evolution.G, evolution.P, evolution.F]]
    def generation: Prototype[Int]
  }
}
