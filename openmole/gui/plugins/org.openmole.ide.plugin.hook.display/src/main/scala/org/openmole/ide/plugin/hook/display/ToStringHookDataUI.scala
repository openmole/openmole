/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.hook.display

import org.openmole.plugin.hook.display._
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.implementation.data.HookDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI

class ToStringHookDataUI(val name: String = "",
                         val toBeHooked: List[PrototypeDataProxyUI] = List.empty) extends HookDataUI {

  def coreClass = classOf[ToStringHook]

  def buildPanelUI = new ToStringHookPanelUI(this)

  override def cloneWithoutPrototype(proxy: PrototypeDataProxyUI) =
    new ToStringHookDataUI(name, toBeHooked.filterNot { _ == proxy })

  def coreObject = util.Try {
    val h = ToStringHook(toBeHooked.map { _.dataUI.coreObject.get }.toSeq: _*)
    initialise(h)
    h.toHook
  }
}
