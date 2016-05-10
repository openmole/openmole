/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.tool

package statistics {

  trait StatisticsPackage extends Stat { stat ⇒

    implicit class StatisticIterableOfDoubleDecorator(s: Seq[Double]) {
      def median: Double = stat.median(s)
      def medianAbsoluteDeviation = stat.medianAbsoluteDeviation(s)
      def average = stat.average(s)
      def meanSquaredError = stat.meanSquaredError(s)
      def rootMeanSquaredError = stat.rootMeanSquaredError(s)
    }

    implicit def statisticArrayOfDoubleDecorator(s: Array[Double]) = new StatisticIterableOfDoubleDecorator(s.toVector)
  }

}

package object statistics extends StatisticsPackage
