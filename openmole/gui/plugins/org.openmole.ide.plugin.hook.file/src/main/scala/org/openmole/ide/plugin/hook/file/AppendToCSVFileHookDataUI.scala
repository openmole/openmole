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

package org.openmole.ide.plugin.hook.file

import org.openmole.plugin.hook.file._
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.implementation.data.HookDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI

class AppendToCSVFileHookDataUI(val name: String = "",
                                val toBeHooked: List[PrototypeDataProxyUI] = List.empty,
                                val fileName: String = "") extends HookDataUI {

  def buildPanelUI = new AppendToCSVFileHookPanelUI(this)

  def coreClass = classOf[AppendToCSVFileHook]

  override def cloneWithoutPrototype(proxy: PrototypeDataProxyUI) =
    new AppendToCSVFileHookDataUI(name, toBeHooked.filterNot(_ == proxy), fileName)

  def coreObject = util.Try {
    val h = AppendToCSVFileHook(
      fileName,
      toBeHooked.map { _.dataUI.coreObject.get }.toSeq: _*)
    initialise(h)
    h
  }

}