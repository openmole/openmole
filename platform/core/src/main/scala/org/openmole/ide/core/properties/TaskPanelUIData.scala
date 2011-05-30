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

import java.awt.Color
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.palette.PaletteElementFactory
import scala.collection.mutable.HashSet

abstract class TaskPanelUIData(n: String) extends PanelUIData(n,Constants.TASK){
  
  var prototypesIn = HashSet.empty[PaletteElementFactory]
  var prototypesOut = HashSet.empty[PaletteElementFactory]  
  var sampling: Option[PaletteElementFactory] = None
  
  def addPrototype(p: PaletteElementFactory, ioType: IOType.Value)= {
    if (p.panelUIData.entityType.equals(Constants.PROTOTYPE)){
      if (ioType.equals(IOType.INPUT)) addPrototypeIn(p)
      else addPrototypeOut(p)
    }
    else throw new GUIUserBadDataError("The entity " + p.panelUIData.name + " of type " + p.panelUIData.entityType + " can not be added as Prototype to the task " + name)
  }

  private def addPrototypeIn(p: PaletteElementFactory)= prototypesIn+= p
  
  private def addPrototypeOut(p: PaletteElementFactory)= prototypesOut+= p
  
  def borderColor: Color
  
  def backgroundColor: Color
}
