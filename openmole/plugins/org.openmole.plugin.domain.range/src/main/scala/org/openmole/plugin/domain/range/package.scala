/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.domain

import java.math.{ BigDecimal, MathContext, RoundingMode }

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.tools.math.BigDecimalOperations

package object range {

  val scale = 128
  private val mc = new MathContext(scale, RoundingMode.HALF_UP)

  trait Log[T] {
    def log(t: T): T
    def exp(t: T): T
  }

  implicit lazy val doubleLog: Log[Double] =
    new Log[Double] {
      def log(t: Double) = math.log(t)
      def exp(t: Double) = math.exp(t)
    }

  implicit lazy val bigDecimalLog: Log[BigDecimal] =
    new Log[BigDecimal] {
      def log(t: BigDecimal) = BigDecimalOperations.ln(t, scale)
      def exp(t: BigDecimal) = BigDecimalOperations.exp(t, scale).setScale(scale, RoundingMode.HALF_UP).round(mc)
    }

  implicit class RangeDomainDecorator[T](r: RangeDomain[T]):
    def step(s: FromContext[T]) = StepRangeDomain[T](r, s)
    def by(s: FromContext[T]) = step(s)
    def size(s: FromContext[Int]) = SizeRangeDomain[T](r, s)
    def logSteps(s: FromContext[Int])(implicit l: Log[T]) = LogRangeDomain[T](r, s)

  trait DefaultStep[T]:
    def step: T

  implicit def defaultStepInt: DefaultStep[Int] = new DefaultStep[Int] { def step = 1 }
  implicit def defaultStepLong: DefaultStep[Long] = new DefaultStep[Long] { def step = 1 }

  object RangeValue {
    implicit def fractionalIsRangeValue[T](implicit fractional: Fractional[T]): RangeValue[T] = new RangeValue[T] {
      override def div(t1: T, t2: T): T = fractional.div(t1, t2)
      override def plus(t1: T, t2: T): T = fractional.plus(t1, t2)
      override def toInt(t: T): Int = fractional.toInt(t)
      override def mult(t1: T, t2: T): T = fractional.times(t1, t2)
      override def fromInt(i: Int): T = fractional.fromInt(i)
      override def minus(t1: T, t2: T): T = fractional.minus(t1, t2)
    }

    implicit def integralIsRangeValue[T](implicit integral: Integral[T]): RangeValue[T] = new RangeValue[T] {
      override def div(t1: T, t2: T): T = integral.quot(t1, t2)
      override def plus(t1: T, t2: T): T = integral.plus(t1, t2)
      override def toInt(t: T): Int = integral.toInt(t)
      override def mult(t1: T, t2: T): T = integral.times(t1, t2)
      override def fromInt(i: Int): T = integral.fromInt(i)
      override def minus(t1: T, t2: T): T = integral.minus(t1, t2)
    }

  }

  trait RangeValue[T] { v ⇒
    def div(t1: T, t2: T): T
    def mult(t1: T, t2: T): T
    def plus(t1: T, t2: T): T
    def minus(t1: T, t2: T): T
    def fromInt(i: Int): T
    def toInt(t: T): Int

    implicit class ops(lhs: T) {
      def +(rhs: T) = plus(lhs, rhs)
      def -(rhs: T) = minus(lhs, rhs)
      def /(rhs: T) = div(lhs, rhs)
      def *(rhs: T) = mult(lhs, rhs)
      def toInt = v.toInt(lhs)
    }
  }

  object IsRangeDomain:
    import org.openmole.tool.collection.DoubleRange
    implicit def doubleRangeIsRange: IsRangeDomain[DoubleRange, Double] = (range: DoubleRange) ⇒ RangeDomain(range.low, range.high)
    implicit def intRangeIsRange: IsRangeDomain[scala.Range, Int] = (range: scala.Range) =>
      val start = range.start
      val end = range.end
      RangeDomain(Math.min(start, end), Math.min(start, end))
      
    implicit def intRangeIsRangeDouble: IsRangeDomain[scala.Range, Double] = (range: scala.Range) =>
      val start = range.start
      val end = range.end
      RangeDomain(Math.min(start, end).toDouble, Math.min(start, end).toDouble)

  trait IsRangeDomain[-D, T]:
    def apply(t: D): RangeDomain[T]

  implicit def isRangeDomainIsBounded[D, T](implicit isRangeDomain: IsRangeDomain[D, T]): BoundedFromContextDomain[D, T] = domain =>
    Domain(
      (isRangeDomain(domain).min, isRangeDomain(domain).max),
      isRangeDomain(domain).inputs,
      isRangeDomain(domain).validate
    )

  implicit def isRangeDomainHasCenter[D, T](implicit isRangeDomain: IsRangeDomain[D, T]): DomainCenterFromContext[D, T] = domain ⇒ RangeDomain.rangeCenter(isRangeDomain(domain))

  @deprecated("Use RangeDomain", "13")
  def Range = RangeDomain

  @deprecated("Use LogRangeDomain", "13")
  def LogRange = LogRangeDomain

  @deprecated("Use SizeRangeDomain", "13")
  def SizeRange = SizeRangeDomain

  @deprecated("Use StepRangeDomain", "13")
  def StepRange = StepRangeDomain

}