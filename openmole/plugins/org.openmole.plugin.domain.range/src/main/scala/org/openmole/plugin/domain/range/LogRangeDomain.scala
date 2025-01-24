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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.workflow.domain

import org.openmole.core.tools.math.BigDecimalOperations
import java.math.{BigDecimal, MathContext, RoundingMode}

object LogRangeDomain:

  private val scale = 128
  private val mc = new MathContext(scale, RoundingMode.HALF_UP)

  object Log:
    given doubleLog: Log[Double] =
      new Log[Double]:
        def log(t: Double) = Math.log(t)
        def exp(t: Double) = Math.exp(t)

    given bigDecimalLog: Log[BigDecimal] =
      new Log[BigDecimal]:
        def log(t: BigDecimal) = BigDecimalOperations.ln(t, scale)
        def exp(t: BigDecimal) = BigDecimalOperations.exp(t, scale).setScale(scale, RoundingMode.HALF_UP).round(mc)

  trait Log[T]:
    def log(t: T): T
    def exp(t: T): T


  given isDiscrete[T]: DiscreteFromContextDomain[LogRangeDomain[T], T] = domain =>
    Domain(
      domain.iterator,
      domain.inputs,
      domain.validate
    )

  given isBounded[T]: BoundedFromContextDomain[LogRangeDomain[T], T] = domain =>
    Domain(
      (domain.min, domain.max),
      domain.inputs,
      domain.validate
    )

  given center[T]: DomainCenterFromContext[LogRangeDomain[T], T] = domain =>
    FromContext: p =>
      import p.*
      import domain.ops.*
      val min = domain.min.from(context)
      val max = domain.max.from(context)
      min + ((max - min) / fromInt(2))



case class LogRangeDomain[T](min: FromContext[T], max: FromContext[T], steps: FromContext[Int])(using lg: LogRangeDomain.Log[T], val ops: RangeDomain.RangeValue[T]):

  def iterator = FromContext: p =>
    import p.*
    
    val logMin: T = lg.log(min.from(context))
    val logMax: T = lg.log(max.from(context))

    val stepsValue = steps.from(context)

    import ops.*

    val logStep = (logMax - logMin) / fromInt(stepsValue - 1)
    Iterator.iterate(logMin)(_ + logStep).map(lg.exp).take(stepsValue)

  def inputs = min.inputs ++ max.inputs ++ steps.inputs
  def validate = min.validate ++ max.validate ++ steps.validate

