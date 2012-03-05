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

package org.openmole.ide.core.implementation.provider

import java.awt.Point
import javax.swing.JMenu
import javax.swing.JMenuItem
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.implementation.action._

class CapsuleMenuProvider(scene: IMoleScene, capsule: ICapsuleUI) extends GenericMenuProvider {
  var encapsulated= false
  var taskMenu= new JMenu
  
  
  def initMenu = {
    items.clear
    val itStart = new JMenuItem("Define as starting capsule")
    val itIS= new JMenuItem("Add an input slot")
    val itRIS = new JMenuItem("Remove an input slot")
    val itR = new JMenuItem("Remove capsule")
    
    itIS.addActionListener(new AddInputSlotAction(capsule))
    itR.addActionListener(new RemoveCapsuleAction(scene,capsule))
    itStart.addActionListener(new DefineMoleStartAction(scene, capsule))
    itRIS.addActionListener(new RemoveInputSlot(capsule))
    
    items+= (itIS,itRIS,itR,itStart)
    
  }
  
  def addTaskMenus= encapsulated= true
  
  override def getPopupMenu(widget: Widget, point: Point)= {
    initMenu
    if (encapsulated) {
      if (capsule.dataProxy.get.dataUI.environment.isDefined) {
        val itRe= new JMenuItem("Detach environment")
      //  itRe.addActionListener(new DetachEnvironmentAction(capsule.dataProxy).peer)
        items+=itRe
      }
    }
    super.getPopupMenu(widget, point)
  }
}