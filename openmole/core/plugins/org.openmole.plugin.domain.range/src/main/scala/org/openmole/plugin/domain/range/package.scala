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

import java.math.{ MathContext, RoundingMode, BigDecimal }
import org.openmole.misc.tools.math.BigDecimalOperations

package object range {

  val scale = 128
  private val mc = new MathContext(scale, RoundingMode.HALF_UP)

  trait Log[T] {
    def log(t: T): T
    def exp(t: T): T
  }

  implicit val doubleLog =
    new Log[Double] {
      def log(t: Double) = math.log(t)
      def exp(t: Double) = math.exp(t)
    }

  implicit val bigDecimalLog =
    new Log[BigDecimal] {
      def log(t: BigDecimal) = BigDecimalOperations.ln(t, scale)
      def exp(t: BigDecimal) = BigDecimalOperations.exp(t, scale).setScale(scale, RoundingMode.HALF_UP).round(mc)
    }

  implicit class RangeDomainDecorator[T](r: Range[T]) {
    def step(s: String) = StepRange[T](r, s)
    def size(s: String) = SizeRange[T](r, s)
  }

}