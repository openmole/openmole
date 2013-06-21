/*Copyright (C) 2013 Fabien De Vienne
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

package org.openmole.plugin.method.abc

import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data._
import scala.collection.immutable.List
import org.apache.commons.math3.linear._
import org.openmole.core.model.task.PluginSet
import org.openmole.core.model.task._
import org.openmole.misc.tools.math._
import org.openmole.core.model.data
import breeze.linalg._

/*object Beaumont {

  def apply(
    name: String,
    target: Seq[Double],
    distance: Prototype[Array[Double]] = Prototype[Double]("distance").toArray)(implicit plugins: PluginSet) = {

    val _distance = distance

    new TaskBuilder { builder ⇒

      addOutput(distance)

      private val _summaryStats = ListBuffer[Prototype[Double]]()

      def addSummaryStat(p: Prototype[Double]) = {
        _summaryStats += p
        addInput(p.toArray)
      }

      def toTask =
        new DistanceTask(name) with builder.Built {
          val summaryStats = _summaryStats.toList.map(_.toArray)
          val summaryStatsTarget = target
          val distances = _distance
        }
    }
  }

}     */

sealed abstract class Beaumont(val name: String, /* val weights: Seq[Double],*/ val context: Context) extends Task with Distance with Selection {

  implicit def listToColumnMatrix(l: List[Double]): DenseMatrix[Double] = new DenseMatrix(l.size, l.toArray)
  /*d = distance; t = thetas; s = summaryStats. They are the nearest points of the target*/
  val (d, t, s) = select(context, distancesValue(context)).unzip3
  val xMatrix = DenseMatrix.ones[Double](t.toList.length, d.toList.length)

  /*à déplacer en amont, sera fait par une autre tache dans le WF*/
  def calculWeight: List[Double] = {
    val W = for (selectedDist ← d) yield 1 - math.pow((selectedDist / d.toList.max), 2)
    W
  }

  val weights = calculWeight

  /*for (r <- xMatrix.rows; c <- xMatrix.cols) yield xMatrix(::, c) := (summaryStatsMatrix(context).toArray[r+1][c-1] - summaryStatsTarget.toArray[c-1])*/

  /*formule n°6 - Beaumont 2002*/
  /*xMatrix.t * xMatrix2.t*/
  val test = inv(xMatrix.t * weights.t) * xMatrix.t * weights.t/* * thetas*/
  /*override def process(context: Context) = {

  }   */
}
