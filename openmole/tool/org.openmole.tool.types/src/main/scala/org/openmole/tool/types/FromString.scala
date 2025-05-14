package org.openmole.tool.types

import java.math.MathContext
import squants._
import squants.time.TimeConversions._
import squants.information._
import squants.information.InformationConversions._

object FromString:

  def apply[T](f: String => T): FromString[T] = new FromString[T]:
    override def apply(s: String) = f(s)

  given doubleFromString: FromString[Double] = FromString[Double](_.toDouble)
  given fileFromString: FromString[java.io.File] = FromString[java.io.File](s => new java.io.File(s))
  given intFromString: FromString[Int] = FromString[Int](_.toInt)
  given longFromString: FromString[Long] = FromString[Long](_.toLong)
  given floatFromString: FromString[Float] = FromString[Float](_.toFloat)
  given bigDecimalFromString: FromString[BigDecimal] = FromString[BigDecimal](s => BigDecimal(s, MathContext.DECIMAL128))
  given bigIntFromString: FromString[BigInt] = FromString[BigInt](s => BigInt(s))
  given javaBigDecimalFromString: FromString[java.math.BigDecimal] = FromString[java.math.BigDecimal](s => BigDecimal(s, MathContext.DECIMAL128).bigDecimal)
  given javaBigIntegerFromString: FromString[java.math.BigInteger] = FromString[java.math.BigInteger](s => BigInt(s).underlying)
  given stringFromString: FromString[String] = FromString[String](identity)
  given booleanFromString: FromString[Boolean] = FromString[Boolean](_.toBoolean)
  given timeFromString: FromString[Time] = FromString[Time](_.toTime.get)
  given memoryFromString: FromString[Information] = FromString[Information](_.toInformation.get)

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

trait FromString[+T]:
  def apply(s: String): T
