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

package org.openmole.ide.plugin.method.sensitivity

import org.openmole.core.model.task.ITask
import org.openmole.ide.core.implementation.builder.{ PuzzleUIMap, SceneFactory }
import org.openmole.plugin.method.sensitivity.FirstOrderEffectTask
import org.openmole.ide.core.implementation.factory.TaskFactoryUI

class FirstOrderEffectTaskFactoryUI extends TaskFactoryUI {
  override def toString = "First Order effect"

  def buildDataUI = new FirstOrderEffectTaskDataUI

  override def category = List("Saltelli")

  def buildDataProxyUI(task: ITask, uiMap: PuzzleUIMap) = {
    val t = SceneFactory.as[FirstOrderEffectTask](task)
    uiMap.task(t, x ⇒ new FirstOrderEffectTaskDataUI(t.name,
      t.modelInputs.map { p ⇒ uiMap.prototypeUI(p).get },
      t.modelOutputs.map { p ⇒ uiMap.prototypeUI(p).get }))
  }
}