package org.openmole.tool.statistics

import org.openmole.tool.types.ToDouble

trait StatisticsPackage extends Stat { stat =>

  implicit class StatisticIterableOfDoubleDecorator[T](s: Seq[T])(implicit td: ToDouble[T]) {
    def median: Double = stat.median(s.map(td.apply))
    def medianAbsoluteDeviation = stat.medianAbsoluteDeviation(s.map(td.apply))
    def average = stat.average(s.map(td.apply))
    def variance = stat.variance(s.map(td.apply))
    def meanSquaredError = stat.meanSquaredError(s.map(td.apply))
    def standardDeviation = stat.standardDeviation(s.map(td.apply))
    def rootMeanSquaredError = stat.rootMeanSquaredError(s.map(td.apply))
    def percentile(p: Double) = stat.percentile(s.map(td.apply), Seq(p)).head

    def normalize = stat.normalize(s.map(td.apply))

    def absoluteDistance[T2](to: Seq[T2])(implicit td2: ToDouble[T2]) = stat.absoluteDistance(s.map(td.apply), to.map(td2.apply))
    def squareDistance[T2](to: Seq[T2])(implicit td2: ToDouble[T2]) = stat.squareDistance(s.map(td.apply), to.map(td2.apply))
    def dynamicTimeWarpingDistance[T2](to: Seq[T2], fast: Boolean = true)(implicit td2: ToDouble[T2]) = stat.dynamicTimeWarpingDistance(s.map(td.apply), to.map(td2.apply))
  }

  implicit def statisticArrayOfDoubleDecorator[T: ToDouble](s: Array[T]): StatisticIterableOfDoubleDecorator[T] = new StatisticIterableOfDoubleDecorator(s.toVector)
}
