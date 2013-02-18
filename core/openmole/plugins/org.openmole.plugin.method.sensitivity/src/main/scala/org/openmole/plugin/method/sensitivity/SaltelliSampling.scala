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

package org.openmole.plugin.method.sensitivity

import org.openmole.misc.tools.service.Scaling._
import java.util.Random
import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.core.model.sampling._
import org.openmole.core.implementation.task.Task._

object SaltelliSampling {

  val aMatrixName = "a"
  val bMatrixName = "b"

  def cMatrixName(p: String) = "c" + p

  def extractValues(allValues: Array[Double], allNames: Array[String], name: String): Array[Double] =
    allValues zip allNames filter { case (_, n) ⇒ n == name } map { case (v, _) ⇒ v }

  def extractValues(allValues: Array[Double], allNames: Array[String], input: Prototype[Double]): (Seq[Double], Seq[Double], Seq[Double]) = {
    val a = extractValues(allValues, allNames, aMatrixName)
    val b = extractValues(allValues, allNames, bMatrixName)
    val c = extractValues(allValues, allNames, cMatrixName(input.name))
    (a, b, c)
  }

  def generateMatrix(
    context: Context,
    samples: Int,
    factors: Seq[Factor[Double, Domain[Double] with Bounds[Double]]],
    rng: Random): Array[Array[Double]] =
    (for (s ← 0 until samples) yield {
      factors.map(f ⇒ rng.nextDouble.scale(f.domain.min(context), f.domain.max(context))).toArray
    }).toArray

  def buildC(
    i: Int,
    a: Array[Array[Double]],
    b: Array[Array[Double]]) =
    a zip b map {
      case (lineOfA, lineOfB) ⇒ buildLineOfC(i, lineOfA, lineOfB)
    }

  def buildLineOfC(i: Int, lineOfA: Array[Double], lineOfB: Array[Double]) =
    (lineOfA zip lineOfB zipWithIndex) map {
      case ((a, b), index) ⇒ if (index == i) a else b
    }

  def toVariables(
    matrix: Array[Array[Double]],
    m: String,
    prototypes: Iterable[Prototype[Double]],
    matrixName: Prototype[String]): List[Iterable[Variable[_]]] =
    matrix.map {
      l ⇒ Variable(matrixName, m) :: (l zip prototypes map { case (v, p) ⇒ Variable(p, v) }).toList
    }.toList

  val matrixName = Prototype[String]("matrixName")

}

import SaltelliSampling._

class SaltelliSampling(
    val samples: Int,
    val matrixName: Prototype[String],
    val factors: Factor[Double, Domain[Double] with Bounds[Double]]*) extends Sampling {

  def this(samples: Int, factors: Factor[Double, Domain[Double] with Bounds[Double]]*) =
    this(
      samples,
      SaltelliSampling.matrixName,
      factors: _*)

  override def prototypes = matrixName :: factors.map { _.prototype }.toList

  override def build(context: Context): Iterator[Iterable[Variable[_]]] = {
    val rng = buildRNG(context)
    val a = generateMatrix(context, samples, factors, rng)
    val b = generateMatrix(context, samples, factors, rng)
    val prototypes = factors.map { _.prototype }

    val cMatrix =
      factors.zipWithIndex.flatMap {
        case (f, i) ⇒ toVariables(buildC(i, a, b), cMatrixName(f.prototype.name), prototypes, matrixName)
      }

    (toVariables(a, aMatrixName, prototypes, matrixName) ++ toVariables(b, bMatrixName, prototypes, matrixName) ++ cMatrix).iterator
  }

}
