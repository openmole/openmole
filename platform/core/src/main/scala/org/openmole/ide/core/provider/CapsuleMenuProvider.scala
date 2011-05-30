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

package org.openmole.ide.core.provider

import java.awt.Point
import javax.swing.JMenu
import javax.swing.JMenuItem
import org.netbeans.api.visual.widget.Widget
import scala.collection.mutable.HashSet
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.workflow.action.AddExistingPrototypeAction
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.workflow.action.AddInputSlotAction
import org.openmole.ide.core.workflow.action.AddTaskAction
import org.openmole.ide.core.workflow.action.DefineMoleStartAction
import org.openmole.ide.core.workflow.action.RemoveCapsuleAction
import org.openmole.ide.core.workflow.implementation.CapsuleViewUI
import org.openmole.ide.core.commons.Constants
import scala.collection.mutable.ListBuffer

class CapsuleMenuProvider(scene: MoleScene, capsuleView: CapsuleViewUI) extends GenericMenuProvider {
  var encapsulated= false
  var inPrototypeMenu= new JMenu
  var outPrototypeMenu= new JMenu
  var taskMenu= new JMenu
  
  val itIS= new JMenuItem("Add an input slot")
  val itR = new JMenuItem("Remove capsule")
  val itStart = new JMenuItem("Define as starting capsule")
  itIS.addActionListener(new AddInputSlotAction(capsuleView))
  itR.addActionListener(new RemoveCapsuleAction(scene,capsuleView))
  itStart.addActionListener(new DefineMoleStartAction(scene, capsuleView))
  
  items+= (itIS,itR,itStart)
  
  def addTaskMenus= encapsulated= true
  
  override def getPopupMenu(widget: Widget, point: Point)= {
    if (encapsulated) {
      val colI= fillPrototypeMenu(IOType.INPUT)
      val colO= fillPrototypeMenu(IOType.OUTPUT)
      if (! colI.isEmpty){
        menus.remove(inPrototypeMenu)  
        menus.remove(outPrototypeMenu)    
        inPrototypeMenu = PopupMenuProviderFactory.addSubMenu("Add an input prototype ", colI)
        outPrototypeMenu = PopupMenuProviderFactory.addSubMenu("Add an output prototype ", colO)
        menus.add(inPrototypeMenu);
        menus.add(outPrototypeMenu)
      }
      if (! ElementFactories.getAll(Constants.TASK).isEmpty){
         menus.remove(taskMenu)
         var colTask = new ListBuffer[JMenuItem]
         ElementFactories.getAll(Constants.TASK).foreach(t=> {
             val it= new JMenuItem(t.panelUIData.name + " :: " + t.panelUIData.coreClass.getSimpleName)
           it.addActionListener(new AddTaskAction(scene,capsuleView, t));
           colTask+= it
          })
      }
    }
    
    super.getPopupMenu(widget, point)
  }
  
  def fillPrototypeMenu(t: IOType.Value)= {
    val prototypeCol = HashSet.empty[JMenuItem]
    ElementFactories.getAll(Constants.PROTOTYPE).foreach(p=> {
        val it= new JMenuItem(p.panelUIData.name + " :: " + p.panelUIData.coreClass.getSimpleName);
        it.addActionListener(new AddExistingPrototypeAction(p, capsuleView, t));
        prototypeCol.add(it)})
    prototypeCol.toSet
  }
}