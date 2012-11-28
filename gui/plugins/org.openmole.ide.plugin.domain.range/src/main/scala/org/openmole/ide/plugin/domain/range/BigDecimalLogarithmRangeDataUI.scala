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
import org.openmole.ide.core.model.data.{ IFactorDataUI, IDomainDataUI }
import org.openmole.ide.misc.tools.util.Types._
import org.openmole.misc.exception.UserBadDataError

case class BigDecimalLogarithmRangeDataUI(val min: String = "0.0",
                                          val max: String = "1.0",
                                          val step: Option[String] = Some("1.0")) extends LogarthmicRangeDataUI {

  val domainType = manifest[BigDecimal]

  override def availableTypes = List(BIG_DECIMAL)

  def coreObject: Domain[BigDecimal] =
    if (min.isEmpty || max.isEmpty || !step.isDefined)
      throw new UserBadDataError("Min, Max ant Step values are required for defining a Logarithm Range Domain")
    else new BigDecimalLogarithmRange(min, max, stepString)

  def coreClass = classOf[BigDecimalLogarithmRangeDataUI]

  def buildPanelUI = new LogarithmicRangePanelUI(this)
}