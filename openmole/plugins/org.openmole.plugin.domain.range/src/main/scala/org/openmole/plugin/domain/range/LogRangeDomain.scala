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

package org.openmole.plugin.domain.range

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.domain

object LogRangeDomain {

  implicit def isDiscrete[T]: DiscreteFromContextDomain[LogRangeDomain[T], T] = domain ⇒
    Domain(
      domain.iterator,
      domain.inputs,
      domain.validate
    )

  implicit def isBounded[T]: BoundedFromContextDomain[LogRangeDomain[T], T] = domain ⇒
    Domain(
      (domain.min, domain.max),
      domain.inputs,
      domain.validate
    )

  implicit def center[T]: DomainCenterFromContext[LogRangeDomain[T], T] = domain ⇒ RangeDomain.rangeCenter(domain.range)

  def apply[T: Log](range: RangeDomain[T], steps: FromContext[Int]) =
    new LogRangeDomain[T](range, steps)

  def apply[T: RangeValue: Log](
    min:   FromContext[T],
    max:   FromContext[T],
    steps: FromContext[Int]
  ): LogRangeDomain[T] =
    LogRangeDomain[T](RangeDomain[T](min, max), steps)

}

sealed class LogRangeDomain[T](val range: RangeDomain[T], val steps: FromContext[Int])(implicit lg: Log[T]) {

  import range._

  def iterator = FromContext { p =>
    import p.*
    
    val logMin: T = lg.log(range.min.from(context))
    val logMax: T = lg.log(range.max.from(context))

    val stepsValue = steps.from(context)

    import ops._

    val logStep = (logMax - logMin) / (fromInt(stepsValue - 1))
    Iterator.iterate(logMin)(_ + logStep).map(lg.exp).take(stepsValue)
  }

  def max = range.max
  def min = range.min

  def inputs = range.inputs ++ steps.inputs
  def validate = range.validate ++ steps.validate

}
