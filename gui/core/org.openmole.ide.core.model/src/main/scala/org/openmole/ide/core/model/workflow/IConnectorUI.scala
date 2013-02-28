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

package org.openmole.ide.core.model.workflow

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.core.model.transition.{ ICondition, Slot, Filter }

trait IConnectorUI extends IConnectorViewUI {
  def source: ICapsuleUI

  def target: IInputSlotWidget

  override def preview: String = {
    val availables = source.outputs
    (availables.size - filteredPrototypes.filter { p ⇒ availables.contains(p) }.size).toString
  }

  def filteredPrototypes: List[IPrototypeDataProxyUI]

  def filteredPrototypes_=(li: List[IPrototypeDataProxyUI])
}
