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

package org.openmole.core.tools.io

import java.math.MathContext
import java.math.{ BigDecimal ⇒ JBigDecimal }
import java.math.{ BigInteger ⇒ JBigInteger }
import java.io.File

object FromString {

  implicit val doubleFromString: FromString[Double] =
    new FromString[Double] {
      def apply(s: String) = s.toDouble
    }

  implicit val fileFromString: FromString[File] =
    new FromString[File] {
      def apply(s: String) = new File(s)
    }

  implicit val intFromString: FromString[Int] =
    new FromString[Int] {
      def apply(s: String) = s.toInt
    }

  implicit val longFromString: FromString[Long] =
    new FromString[Long] {
      def apply(s: String) = s.toLong
    }

  implicit val floatFromString: FromString[Float] =
    new FromString[Float] {
      def apply(s: String) = s.toFloat
    }

  implicit val bigDecimalFromString: FromString[BigDecimal] =
    new FromString[BigDecimal] {
      def apply(s: String) = BigDecimal(s, MathContext.DECIMAL128)
    }

  implicit val bigIntFromString: FromString[BigInt] =
    new FromString[BigInt] {
      def apply(s: String) = BigInt(s)
    }

  implicit val javaBigDecimalFromString: FromString[java.math.BigDecimal] =
    new FromString[java.math.BigDecimal] {
      def apply(s: String) = BigDecimal(s, MathContext.DECIMAL128).bigDecimal
    }

  implicit val javaBigIntegerFromString: FromString[java.math.BigInteger] =
    new FromString[java.math.BigInteger] {
      def apply(s: String) = BigInt(s).underlying
    }

  implicit val stringFromString: FromString[String] =
    new FromString[String] {
      def apply(s: String) = s
    }

  implicit val booleanFromString: FromString[Boolean] =
    new FromString[Boolean] {
      override def apply(s: String): Boolean = s.toBoolean
    }

  implicit val doubleAsIfIntegral = Numeric.DoubleAsIfIntegral
  implicit val bigDecimalAsIfIntegral = Numeric.BigDecimalAsIfIntegral
  implicit val floatAsIfIntegral = Numeric.FloatAsIfIntegral

  implicit val bigJavaBigDecimalAsIfIntegral: Integral[JBigDecimal] = new Integral[JBigDecimal] {
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

  implicit val bigJavaBigIntegerAsIfIntegral: Integral[JBigInteger] = new Integral[JBigInteger] {
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

trait FromString[+T] {
  def apply(s: String): T
}