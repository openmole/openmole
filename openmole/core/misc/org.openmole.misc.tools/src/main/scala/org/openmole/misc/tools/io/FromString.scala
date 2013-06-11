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

package org.openmole.misc.tools.io

import java.math.MathContext
import java.math.{ BigDecimal ⇒ JBigDecimal }
import java.math.{ BigInteger ⇒ JBigInteger }
import java.io.File

object FromString {

  implicit val doubleFromString =
    new FromString[Double] {
      def fromString(s: String) = s.toDouble
    }

  implicit object FileFromString extends FromString[File] {
    def fromString(s: String) = new File(s)
  }

  implicit val intFromString =
    new FromString[Int] {
      def fromString(s: String) = s.toInt
    }

  implicit val longFromString =
    new FromString[Long] {
      def fromString(s: String) = s.toLong
    }

  implicit val floatFromString =
    new FromString[Float] {
      def fromString(s: String) = s.toFloat
    }

  implicit val bigDecimalFromString =
    new FromString[BigDecimal] {
      def fromString(s: String) = BigDecimal(s, MathContext.DECIMAL128)
    }

  implicit val bigIntFromString =
    new FromString[BigInt] {
      def fromString(s: String) = BigInt(s)
    }

  implicit val javaBigDecimalFromString =
    new FromString[java.math.BigDecimal] {
      def fromString(s: String) = BigDecimal(s, MathContext.DECIMAL128).bigDecimal
    }

  implicit val javaBigIntegerFromString =
    new FromString[java.math.BigInteger] {
      def fromString(s: String) = BigInt(s).underlying
    }

  implicit val stringFromString =
    new FromString[String] {
      def fromString(s: String) = s
    }

  implicit val doubleAsIfIntegral = Numeric.DoubleAsIfIntegral
  implicit val bigDecimalAsIfIntegral = Numeric.BigDecimalAsIfIntegral
  implicit val floatAsIfIntegral = Numeric.FloatAsIfIntegral

  implicit val bigJavaBigDecimalAsIfIntegral = new Integral[JBigDecimal] {
    def plus(x: JBigDecimal, y: JBigDecimal): JBigDecimal = x add y
    def minus(x: JBigDecimal, y: JBigDecimal): JBigDecimal = x subtract y
    def times(x: JBigDecimal, y: JBigDecimal): JBigDecimal = x multiply y
    def negate(x: JBigDecimal): JBigDecimal = x.negate
    def fromInt(x: Int): JBigDecimal = new JBigDecimal(x)
    def toInt(x: JBigDecimal): Int = x.intValue
    def toLong(x: JBigDecimal): Long = x.longValue
    def toFloat(x: JBigDecimal): Float = x.floatValue
    def toDouble(x: JBigDecimal): Double = x.doubleValue
    def quot(x: JBigDecimal, y: JBigDecimal): JBigDecimal = (BigDecimal(x) / BigDecimal(y)).underlying
    def rem(x: JBigDecimal, y: JBigDecimal): JBigDecimal = (BigDecimal(x) remainder BigDecimal(y)).underlying
    def compare(x: JBigDecimal, y: JBigDecimal): Int = x compareTo y
  }

  implicit val bigJavaBigIntegerAsIfIntegral = new Integral[JBigInteger] {
    def plus(x: JBigInteger, y: JBigInteger): JBigInteger = x add y
    def minus(x: JBigInteger, y: JBigInteger): JBigInteger = x subtract y
    def times(x: JBigInteger, y: JBigInteger): JBigInteger = x multiply y
    def negate(x: JBigInteger): JBigInteger = x.negate
    def fromInt(x: Int): JBigInteger = BigInt(x).underlying
    def toInt(x: JBigInteger): Int = x.intValue
    def toLong(x: JBigInteger): Long = x.longValue
    def toFloat(x: JBigInteger): Float = x.floatValue
    def toDouble(x: JBigInteger): Double = x.doubleValue
    def quot(x: JBigInteger, y: JBigInteger): JBigInteger = x divide y
    def rem(x: JBigInteger, y: JBigInteger): JBigInteger = x mod y
    def compare(x: JBigInteger, y: JBigInteger): Int = x compareTo y
  }

}

trait FromString[T] {
  def fromString(s: String): T
}