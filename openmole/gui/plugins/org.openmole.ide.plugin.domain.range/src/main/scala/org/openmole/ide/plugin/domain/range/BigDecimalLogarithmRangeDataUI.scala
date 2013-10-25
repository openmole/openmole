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
import org.openmole.plugin.domain.range._
import org.openmole.ide.misc.tools.util.Types._
import org.openmole.misc.tools.io.FromString._
import org.openmole.misc.exception.UserBadDataError

case class BigDecimalLogarithmRangeDataUI(min: String = "0.0",
                                          max: String = "1.0",
                                          step: Option[String] = Some("1.0")) extends LogarthmicRangeDataUI {

  val domainType = manifest[BigDecimal]

  override def availableTypes = List(BIG_DECIMAL)

  def coreObject = util.Try {
    if (min.isEmpty || max.isEmpty || !step.isDefined)
      throw new UserBadDataError("Min, Max ant Step values are required for defining a Logarithm Range Domain")
    else Range[BigDecimal](min, max) logSteps stepString
  }

  def coreClass = classOf[BigDecimalLogarithmRangeDataUI]

  def buildPanelUI = new LogarithmicRangePanelUI(this)
}