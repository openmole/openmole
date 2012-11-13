/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.domain.range

import java.math.BigInteger
import java.math.BigDecimal
import org.openmole.ide.core.model.data.{ IFactorDataUI, IDomainDataUI }
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.misc.exception.UserBadDataError

object GenericRangeDomainDataUI {

  def apply[T](min: String = "0", max: String = "", step: Option[String] = None, log: Boolean, classString: String): IDomainDataUI[_] =
    if (log) {
      classString match {
        case "Double" ⇒ new DoubleLogarithmRangeDataUI(min, max, step)
        case "BigDecimal" ⇒ new BigDecimalLogarithmRangeDataUI(min, max, step)
        case x: Any ⇒ throw new UserBadDataError("The type " + x + " is not supported in logarithm scale")
      }
    } else RangeDomainDataUI(min, max, step, classString)
}

abstract class GenericRangeDomainDataUI[T] extends IDomainDataUI[T] {

  def preview = " on [" + min + "," + max + stepString + "]"

  def stepString = {
    if (step.isDefined) {
      if (step.get.isEmpty) ""
      else "," + step.get
    } else ""
  }

  def isAcceptable(domain: Option[IDomainDataUI[_]]) = false

  def availableTypes: List[String]

  def min: String

  def max: String

  def step: Option[String]
}
