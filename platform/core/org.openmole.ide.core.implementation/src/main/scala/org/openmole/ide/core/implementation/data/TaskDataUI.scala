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

package org.openmole.ide.core.implementation.data

import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.commons.IOType
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.misc.exception.GUIUserBadDataError
import org.openmole.core.implementation.task.Task
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import scala.collection.mutable.HashSet

abstract class TaskDataUI extends ITaskDataUI{
  
  override var prototypesIn = HashSet.empty[IPrototypeDataProxyUI]
  override var prototypesOut = HashSet.empty[IPrototypeDataProxyUI]
  override var sampling: Option[ISamplingDataProxyUI] = None
  override var environment: Option[IEnvironmentDataProxyUI] = None
  
  def addPrototype(p: IPrototypeDataProxyUI, ioType: IOType.Value)= {
    if (p.dataUI.entityType.equals(PROTOTYPE)){
      if (ioType.equals(IOType.INPUT)) addPrototypeIn(p)
      else addPrototypeOut(p)
    }
    else throw new GUIUserBadDataError("The entity " + p.dataUI.name + " of type " + p.dataUI.entityType + " can not be added as Prototype to the task " + name)
  }

  private def addPrototypeIn(p: IPrototypeDataProxyUI)= prototypesIn+= p
  
  private def addPrototypeOut(p: IPrototypeDataProxyUI)= prototypesOut+= p
  
  override def buildTask: Task = {
    val task = coreObject
    prototypesIn.foreach{pp=> task.addInput(pp.dataUI.coreObject)}
    prototypesOut.foreach{pp=> task.addOutput(pp.dataUI.coreObject)}
    task
  }
}
