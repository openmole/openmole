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

package org.openmole.plugin.task.statistic

import org.openmole.core.tools.math.Stat

trait StatisticMethods {
  lazy val average = new StatisticalAggregation[Double] {
    override def apply(s: Seq[Double]): Double = Stat.average(s)
  }

  def confidenceInterval(level: Double) = new StatisticalAggregation[Double] {
    override def apply(s: Seq[Double]): Double = Stat.confidenceInterval(s, level)
  }

  lazy val meanSquareError = new StatisticalAggregation[Double] {
    override def apply(s: Seq[Double]): Double = Stat.meanSquareError(s)
  }

  lazy val medianAbsoluteDeviation = new StatisticalAggregation[Double] {
    override def apply(s: Seq[Double]): Double = Stat.medianAbsoluteDeviation(s)
  }

  lazy val median = new StatisticalAggregation[Double] {
    override def apply(s: Seq[Double]): Double = Stat.median(s)
  }

  lazy val sum = new StatisticalAggregation[Double] {
    override def apply(s: Seq[Double]): Double = s.sum
  }
}
