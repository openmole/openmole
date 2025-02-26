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

package org.openmole.plugin.domain.range

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import cats.implicits._
import org.openmole.core.workflow.domain

object RangeDomain:

  object RangeValue:
    given fractionalIsRangeValue[T](using fractional: Fractional[T]): RangeValue[T] = new RangeValue[T]:
      override def fromInt(i: Int): T = fractional.fromInt(i)

      extension (lhs: T)
        def /(rhs: T): T = fractional.div(lhs, rhs)
        def +(rhs: T): T = fractional.plus(lhs, rhs)
        def toInt: Int = fractional.toInt(lhs)
        def *(rhs: T): T = fractional.times(lhs, rhs)
        def -(rhs: T): T = fractional.minus(lhs, rhs)


    given integralIsRangeValue[T](using integral: Integral[T]): RangeValue[T] = new RangeValue[T]:
      override def fromInt(i: Int): T = integral.fromInt(i)

      extension (lhs: T)
        def /(rhs: T): T = integral.quot(lhs, rhs)
        def +(rhs: T): T = integral.plus(lhs, rhs)
        def toInt: Int = integral.toInt(lhs)
        def *(rhs: T): T = integral.times(lhs, rhs)
        def -(rhs: T): T = integral.minus(lhs, rhs)

  trait RangeValue[T]:
    def fromInt(i: Int): T

    extension (lhs: T)
      def +(rhs: T): T
      def -(rhs: T): T
      def /(rhs: T): T
      def *(rhs: T): T
      def toInt: Int


  object DefaultStep:
    given DefaultStep[Int] = 1
    given DefaultStep[Long] = 1
    given DefaultStep[Double] = 1

    def value[T](d: DefaultStep[T]): T = d

  opaque type DefaultStep[T] = T

  given isBounded[T]: BoundedFromContextDomain[RangeDomain[T], T] =
    BoundedFromContextDomain: domain =>
      Domain(
        (domain.min, domain.max),
        domain.inputs,
        domain.validate
      )

  given hasCenter[T]: DomainCenterFromContext[RangeDomain[T], T] =
    DomainCenterFromContext: domain =>
      FromContext: p =>
        import p.*
        import domain.ops._
        val min = domain.min.from(context)
        val max = domain.max.from(context)
        min + ((max - min) / fromInt(2))

  given isDiscrete[T](using step: DefaultStep[T]): DiscreteFromContextDomain[RangeDomain[T], T] =
    DiscreteFromContextDomain: domain =>
      Domain(
        domain.iterator,
        domain.inputs,
        domain.validate
      )

  def apply[T: RangeValue](
    min: FromContext[T],
    max: FromContext[T],
    step: FromContext[T]
  ): RangeDomain[T] = new RangeDomain[T](min, max, step)


  def apply[T: RangeValue](
    min: FromContext[T],
    max: FromContext[T])(using step: DefaultStep[T]): RangeDomain[T] = new RangeDomain[T](min, max, DefaultStep.value(step))


class RangeDomain[T](val min: FromContext[T], val max: FromContext[T], val step: FromContext[T])(implicit val ops: RangeDomain.RangeValue[T]):
  def inputs = min.inputs ++ max.inputs
  def validate = min.validate ++ max.validate

  def iterator: FromContext[Iterator[T]] =
    FromContext: p =>
      import p.*
      val mi: T = min.from(context)
      val ma: T = max.from(context)
      val st = step.from(context)
      val si = size(mi, ma, st)
      (0 to si).iterator.map(i => mi + (ops.fromInt(i) * st))
    .withValidate { min.validate ++ max.validate ++ step.validate }

  def size(minValue: T, maxValue: T, step: T) =
    import ops.*
    val size = (maxValue - minValue) / step
    if size.toInt < 0 then 0 else size.toInt


