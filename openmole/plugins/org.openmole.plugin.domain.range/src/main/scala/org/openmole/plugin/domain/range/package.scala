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
import org.openmole.core.expansion.FromContext
import org.openmole.core.tools.math.BigDecimalOperations

package object range {

  val scale = 128
  private val mc = new MathContext(scale, RoundingMode.HALF_UP)

  trait Log[T] {
    def log(t: T): T
    def exp(t: T): T
  }

  implicit lazy val doubleLog =
    new Log[Double] {
      def log(t: Double) = math.log(t)
      def exp(t: Double) = math.exp(t)
    }

  implicit lazy val bigDecimalLog =
    new Log[BigDecimal] {
      def log(t: BigDecimal) = BigDecimalOperations.ln(t, scale)
      def exp(t: BigDecimal) = BigDecimalOperations.exp(t, scale).setScale(scale, RoundingMode.HALF_UP).round(mc)
    }

  implicit class RangeDomainDecorator[T](r: Range[T]) {
    def step(s: FromContext[T]) = StepRange[T](r, s)
    def by(s: FromContext[T]) = step(s)
    def size(s: FromContext[Int]) = SizeRange[T](r, s)
    def logSteps(s: FromContext[Int])(implicit l: Log[T]) = LogRange[T](r, s)
  }

  trait DefaultStep[T] {
    def step: T
  }

  implicit def defaultStepInt = new DefaultStep[Int] { def step = 1 }
  implicit def defaultStepLong = new DefaultStep[Long] { def step = 1 }

  object RangeValue {
    implicit def fractionalIsRangeValue[T](implicit fractional: Fractional[T]) = new RangeValue[T] {

      override def div(t1: T, t2: T): T = fractional.div(t1, t2)
      override def plus(t1: T, t2: T): T = fractional.plus(t1, t2)
      override def toInt(t: T): Int = fractional.toInt(t)
      override def mult(t1: T, t2: T): T = fractional.times(t1, t2)
      override def fromInt(i: Int): T = fractional.fromInt(i)
      override def minus(t1: T, t2: T): T = fractional.minus(t1, t2)
    }

    implicit def integralIsRangeValue[T](implicit integral: Integral[T]) = new RangeValue[T] {
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

}