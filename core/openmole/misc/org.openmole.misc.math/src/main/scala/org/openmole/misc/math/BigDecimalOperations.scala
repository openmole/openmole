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

package org.openmole.misc.math

import java.math.BigDecimal
import java.math.BigInteger

object BigDecimalOperations {

  /**
   * Compute x^exponent to a given scale.  Uses the same
   * algorithm as class numbercruncher.mathutils.IntPower.
   * @param x the value x
   * @param exponent the exponent value
   * @param scale the desired scale of the result
   * @return the result value
   */
  def intPower(xVal: BigDecimal, exponentVal: Long, scale: Int): BigDecimal = {
    var x = xVal
    var exponent = exponentVal

    // If the exponent is negative, compute 1/(x^-exponent).
    if (exponent < 0) {
      return BigDecimal.valueOf(1).divide(intPower(x, -exponent, scale), scale,
        BigDecimal.ROUND_HALF_EVEN)
    }

    var power = BigDecimal.valueOf(1)

    // Loop to compute value^exponent.
    while (exponent > 0) {

      // Is the rightmost bit a 1?
      if ((exponent & 1) == 1) {
        power = power.multiply(x).setScale(scale, BigDecimal.ROUND_HALF_EVEN)
      }

      // Square x and shift exponent 1 bit to the right.
      x = x.multiply(x)
        .setScale(scale, BigDecimal.ROUND_HALF_EVEN);
      exponent >>= 1
    }

    return power
  }

  /**
   * Compute the integral root of x to a given scale, x >= 0.
   * Use Newton's algorithm.
   * @param x the value of x
   * @param index the integral root value
   * @param scale the desired scale of the result
   * @return the result value
   */
  def intRoot(xVal: BigDecimal, index: Long, scale: Int): BigDecimal = {
    // Check that x >= 0.
    var x = xVal
    if (x.signum() < 0) throw new IllegalArgumentException("x < 0")

    val sp1 = scale + 1
    val n = x;
    val i = BigDecimal.valueOf(index)
    val im1 = BigDecimal.valueOf(index - 1)
    val tolerance = BigDecimal.valueOf(5).movePointLeft(sp1)

    var xPrev: BigDecimal = null

    // The initial approximation is x/index.
    x = x.divide(i, scale, BigDecimal.ROUND_HALF_EVEN);

    // Loop until the approximations converge
    // (two successive approximations are equal after rounding).
    do {
      // x^(index-1)
      val xToIm1 = intPower(x, index - 1, sp1)

      // x^index
      val xToI = x.multiply(xToIm1).setScale(sp1, BigDecimal.ROUND_HALF_EVEN);

      // n + (index-1)*(x^index)
      val numerator = n.add(im1.multiply(xToI)).setScale(sp1, BigDecimal.ROUND_HALF_EVEN)

      // (index*(x^(index-1))
      val denominator = i.multiply(xToIm1).setScale(sp1, BigDecimal.ROUND_HALF_EVEN)

      // x = (n + (index-1)*(x^index)) / (index*(x^(index-1)))
      xPrev = x
      x = numerator.divide(denominator, sp1, BigDecimal.ROUND_DOWN)

    } while (x.subtract(xPrev).abs().compareTo(tolerance) > 0)

    return x
  }

  /**
   * Compute e^x to a given scale.
   * Break x into its whole and fraction parts and
   * compute (e^(1 + fraction/whole))^whole using Taylor's formula.
   * @param x the value of x
   * @param scale the desired scale of the result
   * @return the result value
   */
  def exp(x: BigDecimal, scale: Int): BigDecimal = {
    // e^0 = 1
    if (x.signum() == 0) return BigDecimal.valueOf(1)

    // If x is negative, return 1/(e^-x).
    else if (x.signum() == -1) {
      return BigDecimal.valueOf(1).divide(exp(x.negate, scale), scale, BigDecimal.ROUND_HALF_EVEN)
    }

    // Compute the whole part of x.
    var xWhole = x.setScale(0, BigDecimal.ROUND_DOWN)

    // If there isn't a whole part, compute and return e^x.
    if (xWhole.signum == 0) return expTaylor(x, scale)

    // Compute the fraction part of x.
    val xFraction = x.subtract(xWhole)

    // z = 1 + fraction/whole
    val z = BigDecimal.valueOf(1).add(xFraction.divide(xWhole, scale, BigDecimal.ROUND_HALF_EVEN))

    // t = e^z
    val t = expTaylor(z, scale)

    val maxLong = BigDecimal.valueOf(Long.MaxValue)
    var result = BigDecimal.valueOf(1);

    // Compute and return t^whole using intPower().
    // If whole > Long.MAX_VALUE, then first compute products
    // of e^Long.MAX_VALUE.
    while (xWhole.compareTo(maxLong) >= 0) {
      result = result.multiply(intPower(t, Long.MaxValue, scale)).setScale(scale, BigDecimal.ROUND_HALF_EVEN)
      xWhole = xWhole.subtract(maxLong)
    }
    return result.multiply(intPower(t, xWhole.longValue, scale)).setScale(scale, BigDecimal.ROUND_HALF_EVEN)
  }

  /**
   * Compute e^x to a given scale by the Taylor series.
   * @param x the value of x
   * @param scale the desired scale of the result
   * @return the result value
   */
  private def expTaylor(x: BigDecimal, scale: Int): BigDecimal = {
    var factorial = BigDecimal.valueOf(1)
    var xPower = x
    var sumPrev: BigDecimal = null

    // 1 + x
    var sum = x.add(BigDecimal.valueOf(1))

    // Loop until the sums converge
    // (two successive sums are equal after rounding).
    var i = 2
    do {
      // x^i
      xPower = xPower.multiply(x).setScale(scale, BigDecimal.ROUND_HALF_EVEN)

      // i!
      factorial = factorial.multiply(BigDecimal.valueOf(i))

      // x^i/i!
      val term = xPower.divide(factorial, scale, BigDecimal.ROUND_HALF_EVEN)

      // sum = sum + x^i/i!
      sumPrev = sum
      sum = sum.add(term)

      i += 1
    } while (sum.compareTo(sumPrev) != 0);

    return sum;
  }

  /**
   * Compute the natural logarithm of x to a given scale, x > 0.
   */
  def ln(x: BigDecimal, scale: Int): BigDecimal = {
    // Check that x > 0.
    if (x.signum() <= 0) {
      throw new IllegalArgumentException("x <= 0")
    }

    // The number of digits to the left of the decimal point.
    val magnitude = x.toString().length() - x.scale() - 1

    if (magnitude < 3) {
      return lnNewton(x, scale)
    } // Compute magnitude*ln(x^(1/magnitude)).
    else {

      // x^(1/magnitude)
      val root = intRoot(x, magnitude, scale)

      // ln(x^(1/magnitude))
      val lnRoot = lnNewton(root, scale)

      // magnitude*ln(x^(1/magnitude))
      return BigDecimal.valueOf(magnitude).multiply(lnRoot).setScale(scale, BigDecimal.ROUND_HALF_EVEN)
    }
  }

  /**
   * Compute the natural logarithm of x to a given scale, x > 0.
   * Use Newton's algorithm.
   */
  private def lnNewton(xVal: BigDecimal, scale: Int): BigDecimal = {
    var x = xVal
    val sp1 = scale + 1
    val n = x
    var term: BigDecimal = null;

    // Convergence tolerance = 5*(10^-(scale+1))
    val tolerance = BigDecimal.valueOf(5).movePointLeft(sp1)

    // Loop until the approximations converge
    // (two successive approximations are within the tolerance).
    do {

      // e^x
      val eToX = exp(x, sp1)

      // (e^x - n)/e^x
      term = eToX.subtract(n)
        .divide(eToX, sp1, BigDecimal.ROUND_DOWN)

      // x - (e^x - n)/e^x
      x = x.subtract(term)

    } while (term.compareTo(tolerance) > 0);
    return x.setScale(scale, BigDecimal.ROUND_HALF_EVEN)
  }

  /**
   * Compute the arctangent of x to a given scale, |x| < 1
   * @param x the value of x
   * @param scale the desired scale of the result
   * @return the result value
   */
  def arctan(x: BigDecimal, scale: Int): BigDecimal = {
    // Check that |x| < 1.
    if (x.abs.compareTo(BigDecimal.valueOf(1)) >= 0) {
      throw new IllegalArgumentException("|x| >= 1")
    }

    // If x is negative, return -arctan(-x).
    if (x.signum() == -1) {
      return arctan(x.negate, scale).negate
    } else {
      return arctanTaylor(x, scale)
    }
  }

  /**
   * Compute the arctangent of x to a given scale
   * by the Taylor series, |x| < 1
   * @param x the value of x
   * @param scale the desired scale of the result
   * @return the result value
   */
  private def arctanTaylor(x: BigDecimal, scale: Int): BigDecimal = {
    val sp1 = scale + 1
    var i = 3
    var addFlag = false

    var power = x
    var sum = x
    var term: BigDecimal = null

    // Convergence tolerance = 5*(10^-(scale+1))
    val tolerance = BigDecimal.valueOf(5).movePointLeft(sp1)

    // Loop until the approximations converge
    // (two successive approximations are within the tolerance).
    do {
      // x^i
      power = power.multiply(x).multiply(x).setScale(sp1, BigDecimal.ROUND_HALF_EVEN)

      // (x^i)/i
      term = power.divide(BigDecimal.valueOf(i), sp1, BigDecimal.ROUND_HALF_EVEN)

      // sum = sum +- (x^i)/i
      sum = if (addFlag) sum.add(term) else sum.subtract(term)

      i += 2
      addFlag = !addFlag;

    } while (term.compareTo(tolerance) > 0)

    return sum;
  }

  /**
   * Compute the square root of x to a given scale, x >= 0.
   * Use Newton's algorithm.
   * @param x the value of x
   * @param scale the desired scale of the result
   * @return the result value
   */
  def sqrt(x: BigDecimal, scale: Int): BigDecimal = {
    // Check that x >= 0.
    if (x.signum() < 0) {
      throw new IllegalArgumentException("x < 0");
    }

    // n = x*(10^(2*scale))
    val n = x.movePointRight(scale << 1).toBigInteger

    // The first approximation is the upper half of n.
    val bits = (n.bitLength() + 1) >> 1
    var ix = n.shiftRight(bits);
    var ixPrev: BigInteger = null

    // Loop until the approximations converge
    // (two successive approximations are equal after rounding).
    do {
      ixPrev = ix

      // x = (x + n/x)/2
      ix = ix.add(n.divide(ix)).shiftRight(1)

    } while (ix.compareTo(ixPrev) != 0)

    return new BigDecimal(ix, scale)
  }

}
