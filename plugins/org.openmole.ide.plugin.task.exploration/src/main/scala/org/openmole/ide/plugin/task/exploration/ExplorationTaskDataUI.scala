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

package org.openmole.ide.plugin.task.exploration

import org.openmole.ide.plugin.task.exploration.ExplorationTaskPanelUI._
import org.openmole.ide.core.model.data.IExplorationTaskDataUI
import org.openmole.ide.core.model.dataproxy.ISamplingCompositionDataProxyUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data._

class ExplorationTaskDataUI(
    val name: String = "",
    override var sampling: Option[ISamplingCompositionDataProxyUI] = None) extends TaskDataUI with IExplorationTaskDataUI {

  def coreObject(inputs: DataSet, outputs: DataSet, parameters: ParameterSet, plugins: PluginSet) = {
    val taskBuilder = sampling match {
      case Some(x: ISamplingCompositionDataProxyUI) ⇒
        ExplorationTask(name, x.dataUI.coreObject)(plugins)
      case None ⇒ throw new UserBadDataError("Sampling missing to instanciate the exploration task " + name)
    }
    taskBuilder addInput inputs
    taskBuilder addOutput outputs
    taskBuilder addParameter parameters
    taskBuilder.toTask
  }

  def coreClass = classOf[ExplorationTask]

  def fatImagePath = "img/explorationTask_fat.png"

  override def imagePath = "img/explorationTask.png"

  def buildPanelUI = new ExplorationTaskPanelUI(this)
}
