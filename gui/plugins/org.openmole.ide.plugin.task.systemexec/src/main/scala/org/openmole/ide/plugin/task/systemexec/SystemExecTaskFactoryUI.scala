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

package org.openmole.ide.plugin.task.systemexec

import org.openmole.ide.core.implementation.panel.ComponentCategories
import org.openmole.ide.core.model.factory.ITaskFactoryUI
import org.openmole.core.model.task.ITask
import org.openmole.ide.core.model.builder.IPuzzleUIMap
import org.openmole.ide.core.implementation.builder.SceneFactory
import org.openmole.plugin.task.systemexec.SystemExecTask

class SystemExecTaskFactoryUI extends ITaskFactoryUI {
  override def toString = "System exec"

  def buildDataUI = new SystemExecTaskDataUI

  def category = ComponentCategories.TASK

  def buildDataProxyUI(task: ITask, uiMap: IPuzzleUIMap) = {
    val t = SceneFactory.as[SystemExecTask](task)
    uiMap.task(t, x ⇒ new SystemExecTaskDataUI(t.name,
      t.directory,
      t.command.head._1,
      t.resources.toList.map { _._2 },
      t.inputFiles.toList.map { p ⇒ (uiMap.prototypeMap(p._1), p._2) },
      t.outputFiles.toList.map { p ⇒ (p._1, uiMap.prototypeMap(p._2)) },
      t.variables.toList.map { p ⇒ uiMap.prototypeMap(p._1) }))
  }
}