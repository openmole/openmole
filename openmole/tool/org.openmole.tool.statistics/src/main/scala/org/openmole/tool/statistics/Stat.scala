/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.tool.statistics

import scala.annotation.tailrec
import scala.math.*

trait Stat:
  def head(sequence: Seq[Double]) = sequence.head
  def sum(sequence: Seq[Double]) = sequence.sum

  def normalise(p: Seq[Double]) =
    val sum = p.sum
    p.map(_ / sum)

  def median(sequence: Seq[Double]) =
    val sortedSerie = sequence.toArray.filterNot(_.isNaN).sorted
    val size = sortedSerie.size
    if (size == sequence.size)
      if (size % 2 == 0) (sortedSerie(size / 2) + sortedSerie((size / 2) - 1)) / 2
      else sortedSerie((size / 2))
    else Double.NaN

  def medianAbsoluteDeviation(sequence: Seq[Double]) =
    val m = median(sequence)
    median(sequence.map { v => math.abs(v - m) })

  def average(sequence: Seq[Double]) = sequence.sum / sequence.size

  def meanSquaredError(sequence: Seq[Double]) = variance(sequence)

  def variance(sequence: Seq[Double]) =
    val avg = average(sequence)
    average(sequence.map { v => math.pow(v - avg, 2) })

  def rootMeanSquaredError(sequence: Seq[Double]) = standardDeviation(sequence)
  def standardDeviation(sequence: Seq[Double]) = sqrt(variance(sequence))

  def normalize(sequence: Seq[Double]) =
    val sum = sequence.sum
    sequence.map(_ / sum)

  /* ------ Difference on series ----- */

  def absoluteDistance(v1: Seq[Double], v2: Seq[Double]): Double =
    (v1 zip v2).map { case (v1v, v2v) => Math.abs(v1v - v2v) }.sum

  def squareDistance(v1: Seq[Double], v2: Seq[Double]): Double =
    (v1 zip v2).map { case (v1v, v2v) => Math.pow(v1v - v2v, 2) }.sum

  def dynamicTimeWarpingDistance(v1: Seq[Double], v2: Seq[Double], fast: Boolean = true): Double =
    import org.openmole.tool.dtw.timeseries.TimeSeries
    import org.openmole.tool.dtw.util.DistanceFunctionFactory

    val ta = new TimeSeries(v1.toArray)
    val tb = new TimeSeries(v2.toArray)
    val df = DistanceFunctionFactory.EUCLIDEAN_DIST_FN

    if (fast) org.openmole.tool.dtw.dtw.FastDTW.getWarpDistBetween(ta, tb, df)
    else org.openmole.tool.dtw.dtw.DTW.getWarpDistBetween(ta, tb, df)

  /**
   * Compute the confidence interval half-width for the given confidence level.
   *
   * @param p the confidence level
   */
  def confidenceInterval(sequence: Seq[Double], p: Double = 0.95) =
    /**
     * Compute the p-th quantile for the "standard normal distribution" function.
     * This function returns an approximation of the "inverse" cumulative
     * standard normal distribution function. I.e., given p, it returns
     * an approximation to the x satisfying p = P{Z <= x} where Z is a
     * random variable from the standard normal distribution.
     * The algorithm uses a minimax approximation by rational functions
     * and the result has a relative error whose absolute value is less
     * than 1.15e-9.
     * Author: Peter J. Acklam (Adapted to Scala by John Miller)
     * (Javascript version by Alankar Misra @ Digital Sutras (alankar@digitalsutras.com))
     * Time-stamp: 2003-05-05 05:15:14
     * E-mail: pjacklam@online.no
     * WWW URL: http://home.online.no/~pjacklam
     *
     * @param p the p-th quantile, e.g., .95 (95%)
     */
    def normalInv(p: Double = .95): Double =
      if (p < 0 || p > 1) throw new IllegalArgumentException("parameter p must be in the range [0, 1]")

      // Coefficients in rational approximations
      val a = Array(-3.969683028665376e+01, 2.209460984245205e+02,
        -2.759285104469687e+02, 1.383577518672690e+02,
        -3.066479806614716e+01, 2.506628277459239e+00)

      val b = Array(-5.447609879822406e+01, 1.615858368580409e+02,
        -1.556989798598866e+02, 6.680131188771972e+01,
        -1.328068155288572e+01)

      val c = Array(-7.784894002430293e-03, -3.223964580411365e-01,
        -2.400758277161838e+00, -2.549732539343734e+00,
        4.374664141464968e+00, 2.938163982698783e+00)

      val d = Array(7.784695709041462e-03, 3.224671290700398e-01,
        2.445134137142996e+00, 3.754408661907416e+00)

      // Define break-points
      val plow = 0.02425
      val phigh = 1 - plow

      // Rational approximation for lower region:
      if (p < plow) {
        val q = sqrt(-2 * log(p))
        return (((((c(0) * q + c(1)) * q + c(2)) * q + c(3)) * q + c(4)) * q + c(5)) /
          ((((d(0) * q + d(1)) * q + d(2)) * q + d(3)) * q + 1)
      } // if

      // Rational approximation for upper region:
      if (phigh < p) {
        val q = sqrt(-2 * log(1 - p))
        return -(((((c(0) * q + c(1)) * q + c(2)) * q + c(3)) * q + c(4)) * q + c(5)) /
          ((((d(0) * q + d(1)) * q + d(2)) * q + d(3)) * q + 1)
      } // if

      // Rational approximation for central region:
      val q = p - 0.5
      val r = q * q
      (((((a(0) * r + a(1)) * r + a(2)) * r + a(3)) * r + a(4)) * r + a(5)) * q /
        (((((b(0) * r + b(1)) * r + b(2)) * r + b(3)) * r + b(4)) * r + 1)
    // normalInv

    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Compute the p-th quantile for "Student's t distribution" function.
     * This function returns an approximation of the "inverse" cumulative
     * Student's t distribution function. I.e., given p, it returns
     * an approximation to the x satisfying p = P{T <= x} where T is a
     * random variable from Student's t distribution.
     * From K. Pawlikowski (www.cosc.canterbury.ac.nz).
     * This function computes the upper p-th quantile of the t distribution (the
     * value of t for which the area under the curve from t to +infinity is equal
     * to p). It is a transliteration of the 'STUDTP' function given in Appendix C
     * of "Principles of Discrete Event Simulation", G. S. Fishman, Wiley, 1978.
     *
     * @param p the p-th quantile, e.g., 95 (95%)
     * @param df the degrees of freedom
     */
    def studentTInv(p: Double = .95, df: Int = 10): Double =
      if (p < 0 || p > 1) throw new IllegalArgumentException("parameter p must be in the range [0, 1]")
      if (df <= 0) throw new IllegalArgumentException("parameter df must be positive")

      val z1 = abs(normalInv(p))
      val z2 = z1 * z1

      val h = Array[Double](
        0.25 * z1 * (z2 + 1.0),
        0.010416667 * z1 * ((5.0 * z2 + 16.0) * z2 + 3.0),
        0.002604167 * z1 * (((3.0 * z2 + 19.0) * z2 + 17.0) * z2 - 15.0),
        0.000010851 * z1 * ((((79.0 * z2 + 776.0) * z2 + 1482.0) * z2 - 1920.0) * z2 - 945.0)
      )

      var x = 0.0
      for (i ← h.length - 1 to 0 by -1) x = (x + h(i)) / df
      if (p >= 0.5) z1 + x else -(z1 + x)
    // studentTInv

    val n = sequence.size
    val df = n - 1 // degrees of freedom
    if df < 1
    then 0.0 // flaw ("interval", "must have at least 2 observations")
    else
      val pp = 1 - (1 - p) / 2.0
      // e.g., .95 --> .975 (two tails)
      val t = studentTInv(pp, df)
      t * rootMeanSquaredError(sequence) / sqrt(n.toDouble)

  end confidenceInterval


  def ksTest(d1: Seq[Double], d2: Seq[Double]): Option[Double] = kolmogorovSmirnovTest(d1, d2)
  def kolmogorovSmirnovTest(d1: Seq[Double], d2: Seq[Double]): Option[Double] =
    if d1.size < 2 || d2.size < 2
    then None
    else
      import org.apache.commons.math3.stat.inference.*
      val test = new KolmogorovSmirnovTest()
      Some:
        test.kolmogorovSmirnovTest(normalise(d1).toArray, normalise(d2).toArray)

  def ksDivergence(p: Seq[Double], q: Seq[Double]) = kolmogorovSmirnovDivergence(p, q)
  def kolmogorovSmirnovDivergence(p: Seq[Double], q: Seq[Double]) =
    (normalise(p) zip normalise(q)).map((x, y) => math.abs(x - y)).max

  def ksTestGaussian(data: Seq[Double], mu: Double, sigma: Double): Option[Double] =
    if data.size < 2
    then None
    else
      import org.apache.commons.math3.stat.inference._
      import org.apache.commons.math3.distribution._

      val test = new KolmogorovSmirnovTest()
      Some(test.kolmogorovSmirnovTest(new NormalDistribution(null, mu, sigma), data.toArray))

  def klDivergence(p1: Seq[Double], p2: Seq[Double]) = kullbackLeiblerDivergence(p1, p2)
  def kullbackLeiblerDivergence(p: Seq[Double], q: Seq[Double], epsilon: Option[Double] = Some(1e-10)): Double =
    val epsilonValue = epsilon.map(e => Math.max(0, e)).getOrElse(0.0)
    (normalise(p) zip normalise(q)).map: (x, y) =>
      val yValue = Math.max(epsilonValue, y)
      if yValue == 0 || x == 0 then 0 else x * math.log(x / yValue)
    .sum

  def jeffreysDivergence(p: Seq[Double], q: Seq[Double], epsilon: Option[Double] = Some(1e-10)): Double =
    kullbackLeiblerDivergence(p, q, epsilon) + kullbackLeiblerDivergence(q, p, epsilon)

  def probabilityDistribution(s: Seq[Double], beans: Int) =
    val ssorted = s.sorted
    val smin = ssorted.head
    val smax = ssorted.last
    val step = (smax - smin) / beans

    @tailrec def recurse(lowBound: BigDecimal, ssorted: List[Double], beans: List[Int]): List[Int] =
      val highBound = lowBound + step
      if highBound >= smax
      then (ssorted.size :: beans).reverse
      else
        def bean = ssorted.takeWhile(_ <= highBound).size
        recurse(highBound, ssorted.dropWhile(_ <= highBound), bean :: beans)

    val size = ssorted.size
    recurse(BigDecimal(smin), ssorted.toList, List()).map(_.toDouble / size)

  def percentile(s: Seq[Double], n: Double): Double =
    percentile(s, Seq(n)).head

  def percentile(s: Seq[Double], n: Seq[Double]) = 
    import org.apache.commons.math3.stat.descriptive.rank.Percentile
    val c = new Percentile()
    c.setData(s.toArray)
    n.map(n => c.evaluate(n))

