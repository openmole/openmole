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

package org.openmole.ide.core.implementation.workflow

import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.implementation.provider.DnDTaskIntoCapsuleProvider
import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyUI
import org.openmole.ide.core.implementation.provider.CapsuleMenuProvider
import org.openmole.ide.core.model.commons.CapsuleType._
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.implementation.dataproxy.Proxys

class CapsuleUI(val scene: MoleScene) extends Widget(scene) with ICapsuleUI{

  var dataProxy: Option[ITaskDataProxyUI] = None
  var nbInputSlots: Int = 0
  var capsuleType = CAPSULE
  var startingCapsule = false
  
  createActions(scene.MOVE).addAction (ActionFactory.createMoveAction)
  
  val connectableWidget= new ConnectableWidget(scene,this)
  val dndTaskIntoCapsuleProvider = new DnDTaskIntoCapsuleProvider(scene, this)
  val capsuleMenuProvider= new CapsuleMenuProvider(scene, this)
  
  addChild(connectableWidget)
        
  getActions.addAction(ActionFactory.createPopupMenuAction(capsuleMenuProvider))
  getActions.addAction(ActionFactory.createAcceptAction(dndTaskIntoCapsuleProvider))
    
  override def widget = this
  
  override def encapsule(dpu: ITaskDataProxyUI)= {
    setDataProxy(dpu)
    if (capsuleType == EXPLORATION_TASK) connectableWidget.addSampling
    dndTaskIntoCapsuleProvider.encapsulated= true
    capsuleMenuProvider.addTaskMenus
  }
  
  def addInputSlot(on: Boolean): IInputSlotWidget =  {
    if (on || startingCapsule) clearInputSlots(on)
    
    nbInputSlots+= 1
    val im = new InputSlotWidget(scene,this,nbInputSlots,startingCapsule)
    connectableWidget.addInputSlot(im)
    scene.validate
    scene.refresh
    im
  }

  private def clearInputSlots(on: Boolean) = {
    startingCapsule = on
    nbInputSlots= 0
    connectableWidget.clearInputSlots
  }
  
  def removeInputSlot= nbInputSlots-= 1
  
  def setDataProxy(dpu: ITaskDataProxyUI)={
    dataProxy= Some(dpu)
    if (Proxys.isExplorationTaskData(dpu.dataUI)) capsuleType = EXPLORATION_TASK else capsuleType = BASIC_TASK
  }
}
