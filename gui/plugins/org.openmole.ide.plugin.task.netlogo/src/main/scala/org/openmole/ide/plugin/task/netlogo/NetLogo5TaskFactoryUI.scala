/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.task.netlogo

import org.openmole.ide.core.implementation.panel.ComponentCategories
import org.openmole.ide.core.model.factory.ITaskFactoryUI
import org.openmole.core.model.task.ITask
import org.openmole.ide.core.model.builder.IPuzzleUIMap
import org.openmole.plugin.task.netlogo5.NetLogo5Task
import org.openmole.ide.core.implementation.builder.SceneFactory

class NetLogo5TaskFactoryUI extends ITaskFactoryUI {
  override def toString = "NetLogo5"

  def buildDataUI = new NetLogo5TaskDataUI

  def category = ComponentCategories.ABM_TASK

  def buildDataProxyUI(task: ITask, uiMap: IPuzzleUIMap) = {
    val t = SceneFactory.as[NetLogo5Task](task)
    val embededWS = t.workspace.location match {
      case Right(r) ⇒ true
      case Left(l) ⇒ false
    }
    uiMap.task(t, x ⇒ new NetLogo4TaskDataUI(t.name,
      embededWS,
      t.scriptPath,
      t.launchingCommands.mkString("\n"),
      t.netLogoInputs.toList.map { p ⇒ (uiMap.prototype(p._1), p._2) },
      t.netLogoOutputs.toList.map { p ⇒ (p._1, uiMap.prototype(p._2)) },
      t.resources.map { _._2 }.toList))
  }
}
