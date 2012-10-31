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

import java.math.BigDecimal
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.plugin.domain.range.BigDecimalLogarithmRange
import org.openmole.core.model.domain.Domain
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI

class BigDecimalLogarithmRangeDataUI(val min: String = "0.0", val max: String = "", val step: Option[String] = None) extends LogarthmicRangeDataUI[BigDecimal] {

  def availableTypes = List("BigDecimal")

  def coreObject(prototype: IPrototypeDataProxyUI): Domain[BigDecimal] = new BigDecimalLogarithmRange(min, max, stepString)

  def coreClass = classOf[BigDecimalLogarithmRangeDataUI]

  def buildPanelUI(p: IPrototypeDataProxyUI) = new LogarithmicRangePanelUI(this, p)

  override def toString = "Log Range"
}