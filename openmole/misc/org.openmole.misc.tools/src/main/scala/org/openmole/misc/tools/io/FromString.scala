/*
 * Copyright (C) 2012 reuillon
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

object FromString {


  implicit val doubleFromString = 
    new FromString[Double] {
      def fromString(s: String) = s.toDouble
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
  
  implicit val doubleAsIfIntegral = Numeric.DoubleAsIfIntegral
  implicit val bigDecimalAsIfIntegral = Numeric.BigDecimalAsIfIntegral
  implicit val floatAsIfIntegral = Numeric.FloatAsIfIntegral
  
}

trait FromString[T] {
  def fromString(s: String): T
}