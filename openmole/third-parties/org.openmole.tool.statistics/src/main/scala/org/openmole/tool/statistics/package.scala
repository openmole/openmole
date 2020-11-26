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

  import org.openmole.tool.types.ToDouble

  trait StatisticsPackage extends Stat { stat ⇒

    implicit class StatisticIterableOfDoubleDecorator[T](s: Seq[T])(implicit td: ToDouble[T]) {
      def median: Double = stat.median(s.map(td.apply))
      def medianAbsoluteDeviation = stat.medianAbsoluteDeviation(s.map(td.apply))
      def average = stat.average(s.map(td.apply))
      def variance = stat.variance(s.map(td.apply))
      def meanSquaredError = stat.meanSquaredError(s.map(td.apply))
      def standardDeviation = stat.standardDeviation(s.map(td.apply))
      def rootMeanSquaredError = stat.rootMeanSquaredError(s.map(td.apply))

      def normalize = stat.normalize(s.map(td.apply))

      def absoluteDistance[T2](to: Seq[T2])(implicit td2: ToDouble[T2]) = stat.absoluteDistance(s.map(td.apply), to.map(td2.apply))
      def squareDistance[T2](to: Seq[T2])(implicit td2: ToDouble[T2]) = stat.squareDistance(s.map(td.apply), to.map(td2.apply))
      def dynamicTimeWarpingDistance[T2](to: Seq[T2], fast: Boolean = true)(implicit td2: ToDouble[T2]) = stat.dynamicTimeWarpingDistance(s.map(td.apply), to.map(td2.apply))
    }

    implicit def statisticArrayOfDoubleDecorator[T: ToDouble](s: Array[T]) = new StatisticIterableOfDoubleDecorator(s.toVector)
  }

}

package object statistics extends StatisticsPackage
