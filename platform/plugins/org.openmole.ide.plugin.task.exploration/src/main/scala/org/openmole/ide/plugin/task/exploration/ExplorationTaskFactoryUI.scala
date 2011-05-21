/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

import java.awt.Color
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.ide.core.properties.PanelUIData
import org.openmole.core.model.sampling.ISampling

class ExplorationTaskFactoryUI extends ITaskFactoryUI {
  
  override def coreObject(pud: PanelUIData) = {
    new Object
  }
  //  if (panelData.samplingFactory.isDefined) new ExplorationTask(panelData.name,panelData.samplingFactory.get.coreObject.asInstanceOf[ISampling])
 //   else throw new GUIUserBadDataError("Sampling missing to instanciate the exploration task " + panelData.name)
  
  override def coreClass= classOf[ExplorationTask]
  
  override def borderColor = new Color(255,102,0)
  
  override def backgroundColor = new Color(255,102,0,128)
  
  override def imagePath = "img/thumb/explorationTaskSmall.png"
  
  override def buildPanelUI = new ExplorationTaskPanelUI
  
  override def buildPanelUIData = new ExplorationTaskPanelUIData
}

  
  