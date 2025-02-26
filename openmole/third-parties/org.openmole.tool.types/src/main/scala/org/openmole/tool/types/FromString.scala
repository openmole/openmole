package org.openmole.tool.types

import java.math.MathContext
import squants._
import squants.time.TimeConversions._
import squants.information._
import squants.information.InformationConversions._

trait FromString[+T] {
  def apply(s: String): T
}

object FromString {

  def apply[T](f: String => T): FromString[T] = new FromString[T] {
    override def apply(s: String) = f(s)
  }

  implicit def doubleFromString: FromString[Double] = FromString[Double](_.toDouble)
  implicit def fileFromString: FromString[java.io.File] = FromString[java.io.File](s => new java.io.File(s))
  implicit def intFromString: FromString[Int] = FromString[Int](_.toInt)
  implicit def longFromString: FromString[Long] = FromString[Long](_.toLong)
  implicit def floatFromString: FromString[Float] = FromString[Float](_.toFloat)
  implicit def bigDecimalFromString: FromString[BigDecimal] = FromString[BigDecimal](s => BigDecimal(s, MathContext.DECIMAL128))
  implicit def bigIntFromString: FromString[BigInt] = FromString[BigInt](s => BigInt(s))
  implicit def javaBigDecimalFromString: FromString[java.math.BigDecimal] = FromString[java.math.BigDecimal](s => BigDecimal(s, MathContext.DECIMAL128).bigDecimal)
  implicit def javaBigIntegerFromString: FromString[java.math.BigInteger] = FromString[java.math.BigInteger](s => BigInt(s).underlying)
  implicit def stringFromString: FromString[String] = FromString[String](identity)
  implicit def booleanFromString: FromString[Boolean] = FromString[Boolean](_.toBoolean)
  implicit def timeFromString: FromString[Time] = FromString[Time](_.toTime.get)
  implicit def memoryFromString: FromString[Information] = FromString[Information](_.toInformation.get)

  /*implicit val doubleAsIfIntegral = Numeric.DoubleAsIfIntegral
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
  }*/

}