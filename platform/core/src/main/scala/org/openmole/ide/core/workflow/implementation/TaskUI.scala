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

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.control.MoleScenesManager
import java.awt.Color
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.properties.PanelUI
import org.openmole.ide.core.workflow.model.IEntityUI
import scala.collection.mutable.HashSet

//class TaskUI(var name: String, val coreClass: Class[_],val borderColor: Color,val backgroundColor: Color,val panel: PanelUI) extends IEntityUI {
//  def this(coreClass: Class[_],borderColor: Color, backgroundColor: Color,panel: PanelUI)= this(MoleScenesManager.incrementTaskName,coreClass,borderColor,backgroundColor,panel)
  
//class TaskUI(taskFactory: ITaskFactoryUI) extends IEntityUI {
class TaskUI(factoryUI: IFactoryUI) extends EntityUI(factoryUI,Constants.TASK) {
  //override def factoryUI = elemfactory.asInstanceOf[ITaskFactoryUI]
   
  var prototypesIn = HashSet.empty[IEntityUI]
  var prototypesOut = HashSet.empty[IEntityUI]  
  
  def addPrototype(p: IEntityUI, ioType: IOType.Value)= {
    if (p.entityType.equals(Constants.PROTOTYPE)){
      if (ioType.equals(IOType.INPUT)) addPrototypeIn(p)
      else addPrototypeOut(p)
    }
    else throw new GUIUserBadDataError("The entity " + p.panelUIData.name + " of type " + p.entityType + " can not be added as Prototype to the task " + panelUIData.name)
  }

  private def addPrototypeIn(p: IEntityUI)= prototypesIn+= p
  
  private def addPrototypeOut(p: IEntityUI)= prototypesOut+= p
}
//
//public class TaskUI extends EntityUI{
//
//    public TaskUI() {
//    super();
//    }
//    
//    public TaskUI(String name, Class type) {
//        super(name,type);
//    }
//}