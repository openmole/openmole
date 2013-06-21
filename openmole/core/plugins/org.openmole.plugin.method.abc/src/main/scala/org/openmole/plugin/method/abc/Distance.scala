/*Copyright (C) 2013 Mathieu Leclaire
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

import org.openmole.core.model.data._
import org.apache.commons.math3.stat.descriptive._
import org.openmole.misc.exception.UserBadDataError

trait Distance <: SummaryStats {

  def summaryStatsTarget: Seq[Double]

  def distancesValue(context: Context): Seq[Double] =
    for {
      summaryStat ← summaryStatsMatrix(context)
    } yield {
      val variance = new DescriptiveStatistics(summaryStat.toArray).getVariance
      (summaryStat zip summaryStatsTarget).map {
        case (a, b) ⇒ Math.pow(a - b, 2) / variance
      }.reduceLeftOption(_ + _).getOrElse(throw new UserBadDataError("Summary stat shouldn't be empty"))
    }

}