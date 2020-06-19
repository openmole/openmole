package org.openmole.tool.collection

import scala.collection.immutable.NumericRange

trait DoubleIsConflicted extends Numeric[Double] {
  def plus(x: Double, y: Double): Double = x + y
  def minus(x: Double, y: Double): Double = x - y
  def times(x: Double, y: Double): Double = x * y
  def negate(x: Double): Double = -x
  def fromInt(x: Int): Double = x.toDouble
  def toInt(x: Double): Int = x.toInt
  def toLong(x: Double): Long = x.toLong
  def toFloat(x: Double): Float = x.toFloat
  def toDouble(x: Double): Double = x

  def parseString(s: String) = util.Try(s.toDouble).toOption

  // logic in Numeric base trait mishandles abs(-0.0)
  override def abs(x: Double): Double = math.abs(x)
}

class DoubleAsIfIntegral extends DoubleIsConflicted with Integral[Double] with Ordering.Double.TotalOrdering {
  def quot(x: Double, y: Double): Double = (BigDecimal(x) quot BigDecimal(y)).doubleValue
  def rem(x: Double, y: Double): Double = (BigDecimal(x) remainder BigDecimal(y)).doubleValue
}

class DoubleRange(d: Double) {
  implicit val doubleAsIfIntegral = new DoubleAsIfIntegral
  def until(u: Double) = NumericRange(d, u, 1.0)
  def to(u: Double) = NumericRange.inclusive(d, u, 1.0)
}