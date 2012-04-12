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

package org.openmole.ide.plugin.task.exploration

import java.awt.Color
import org.openmole.ide.plugin.task.exploration.ExplorationTaskPanelUI._
import org.openmole.ide.core.model.dataproxy.ISamplingDataProxyUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.ide.core.implementation.data._

class ExplorationTaskDataUI(val name: String="",
                            override var sampling : Option[ISamplingDataProxyUI] = None) extends AbstractExplorationTaskDataUI{
   
  def coreObject =
    sampling match {
      case Some(x: ISamplingDataProxyUI) => new ExplorationTask(name,x.dataUI.coreObject)
      case None => throw new UserBadDataError("Sampling missing to instanciate the exploration task " + name)
    } 
  
  def coreClass= classOf[ExplorationTask]
  
  def fatImagePath = "img/explorationTask_fat.png"
  
  def buildPanelUI = new ExplorationTaskPanelUI(this)
  
  def borderColor = new Color(255,102,0)
  
  def backgroundColor = new Color(255,102,0,128)
}
