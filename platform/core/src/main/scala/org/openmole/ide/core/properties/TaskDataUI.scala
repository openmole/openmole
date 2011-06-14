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

package org.openmole.ide.core.properties

import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.palette._
import scala.collection.mutable.HashSet

abstract class TaskDataUI extends ITaskDataUI{
  
  var prototypesIn = HashSet.empty[PrototypeDataProxyUI]
  var prototypesOut = HashSet.empty[PrototypeDataProxyUI]
  var sampling: Option[SamplingDataProxyUI] = None
  var environment: Option[EnvironmentDataProxyUI] = None
  
  def addPrototype(p: PrototypeDataProxyUI, ioType: IOType.Value)= {
    if (p.dataUI.entityType.equals(Constants.PROTOTYPE)){
      if (ioType.equals(IOType.INPUT)) addPrototypeIn(p)
      else addPrototypeOut(p)
    }
    else throw new GUIUserBadDataError("The entity " + p.dataUI.name + " of type " + p.dataUI.entityType + " can not be added as Prototype to the task " + name)
  }

  private def addPrototypeIn(p: PrototypeDataProxyUI)= prototypesIn+= p
  
  private def addPrototypeOut(p: PrototypeDataProxyUI)= prototypesOut+= p
}
