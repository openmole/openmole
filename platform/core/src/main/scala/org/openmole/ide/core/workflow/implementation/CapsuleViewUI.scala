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

import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.provider.DnDTaskIntoCapsuleProvider
import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.properties.TaskPanelUIData
import org.openmole.ide.core.provider.CapsuleMenuProvider
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.workflow.implementation.paint.ConnectableWidget
import org.openmole.ide.core.workflow.implementation.paint.ISlotWidget
import org.openmole.ide.core.workflow.model.ICapsuleModelUI
import org.openmole.ide.core.workflow.model.ICapsuleView

class CapsuleViewUI(val scene: MoleScene,val capsuleModel: ICapsuleModelUI) extends Widget(scene) with ICapsuleView{

  createActions(scene.MOVE).addAction (ActionFactory.createMoveAction)
  
  val connectableWidget= new ConnectableWidget(scene,this)
  val dndTaskIntoCapsuleProvider = new DnDTaskIntoCapsuleProvider(scene, this)
  val capsuleMenuProvider= new CapsuleMenuProvider(scene, this)
  
  addChild(connectableWidget)
        
  getActions.addAction(ActionFactory.createPopupMenuAction(capsuleMenuProvider))
  getActions.addAction(ActionFactory.createAcceptAction(dndTaskIntoCapsuleProvider))
  
  def defineStartingCapsule(on: Boolean){
    capsuleModel.defineStartingCapsule(on)
    connectableWidget.clearInputSlots
    connectableWidget.addInputSlot(new ISlotWidget(scene,this,1,on))
  }
  
  override def encapsule(pef: PaletteElementFactory)= {
    capsuleModel.setDataProxy(pef)
    if (ElementFactories.isExplorationTaskFactory(pef.panelUIData)) connectableWidget.addSampling
    dndTaskIntoCapsuleProvider.encapsulated= true
    capsuleMenuProvider.addTaskMenus
  }
  
  def addInputSlot: ISlotWidget =  {
    capsuleModel.addInputSlot
    val im = new ISlotWidget(scene, this,capsuleModel.nbInputSlots,capsuleModel.startingCapsule)
    connectableWidget.addInputSlot(im)
    scene.validate
    scene.refresh
    im
  }

}