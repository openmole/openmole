/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.action

import org.openmole.ide.core.implementation.control.MoleScenesManager
import org.openmole.ide.core.implementation.control.TabManager
import scala.swing.Action

class CleanAndBuildExecutionAction(text: String) extends Action(text){
  override def apply = {
    if (TabManager.currentScene.isDefined){
    MoleScenesManager.removeAllExecutionMoleScene(TabManager.currentScene.get)
    TabManager.displayExecutionMoleScene(MoleScenesManager.addExecutionMoleScene(TabManager.currentScene.get))}
  }
}
