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

package org.openmole.ide.plugin.task.exploration

import org.openmole.ide.core.implementation.panel.ComponentCategories
import org.openmole.ide.core.model.factory.ITaskFactoryUI
import org.openmole.core.model.task.ITask
import org.openmole.ide.core.implementation.builder._
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.ide.core.implementation.builder.SceneFactory
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.builder.IPuzzleUIMap
import org.openmole.ide.core.implementation.dataproxy.{ SamplingCompositionDataProxyUI, TaskDataProxyUI }
import org.openmole.ide.core.implementation.sampling.SamplingCompositionDataUI

class ExplorationTaskFactoryUI extends ITaskFactoryUI {
  override def toString = "Exploration"

  def category = ComponentCategories.TASK

  def buildDataUI = new ExplorationTaskDataUI

  def buildDataProxyUI(task: ITask, uiMap: IPuzzleUIMap) = {
    val t = SceneFactory.as[ExplorationTask](task)
    uiMap.task(task, x ⇒ (new ExplorationTaskDataUI(t.name, Some(uiMap.sampling(t.sampling, s ⇒ new SamplingCompositionDataUI(task.name + "Sampling"))))))
  }

}

