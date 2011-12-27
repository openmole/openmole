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
import org.openmole.ide.core.implementation.provider.CapsuleMenuProvider
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.commons.CapsuleType._
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.collection.mutable.HashMap

class CapsuleUI(val scene: IMoleScene, var dataProxy: Option[ITaskDataProxyUI],var capsuleType:CapsuleType,var startingCapsule: Boolean) extends Widget(scene.graphScene) with ICapsuleUI{
  def this(sc: IMoleScene) = this (sc,None,CAPSULE,sc.manager.capsules.size == 0)
    
  createActions(MOVE).addAction (ActionFactory.createMoveAction)
  
  var nbInputSlots = 0
  val connectableWidget= new ConnectableWidget(scene,this)
  val dndTaskIntoCapsuleProvider = new DnDTaskIntoCapsuleProvider(scene, this)
  val capsuleMenuProvider= new CapsuleMenuProvider(scene, this)
  override var detailedView = false
  
  addChild(connectableWidget)
        
  getActions.addAction(ActionFactory.createPopupMenuAction(capsuleMenuProvider))
  getActions.addAction(ActionFactory.createAcceptAction(dndTaskIntoCapsuleProvider))
    
  def widget = this
  
  def copy(sc: IMoleScene) = {
    var slotMapping = new HashMap[IInputSlotWidget,IInputSlotWidget]
    val c = new CapsuleUI(sc,dataProxy,capsuleType,startingCapsule)
    connectableWidget.islots.foreach(i=>slotMapping+=i->c.addInputSlot(false))
    if (dataProxy.isDefined) {
      c.setDataProxy(dataProxy.get)
    } 
    else capsuleType = BASIC_TASK
    (c,slotMapping)
  }
  
  def defineAsStartingCapsule(b : Boolean) = {
    startingCapsule = b
    connectableWidget.islots.foreach{ isw=>
      isw.setStartingSlot(b)}
    scene.validate
    scene.refresh
  }
  
  def encapsule(dpu: ITaskDataProxyUI)= {
    setDataProxy(dpu)
    capsuleMenuProvider.addTaskMenus
  }
  
  
  def addInputSlot(on: Boolean): IInputSlotWidget =  {
    if (on) startingCapsule = on
    
    nbInputSlots+= 1
    val im = new InputSlotWidget(scene,this,nbInputSlots,on)
    connectableWidget.addInputSlot(im)
    scene.validate
    scene.refresh
    im
  }

  def removeInputSlot= {
    nbInputSlots-= 1
    connectableWidget.removeFirstInputSlot
  }
  
  def setDataProxy(dpu: ITaskDataProxyUI)={
    dataProxy= Some(dpu)
    if (Proxys.isExplorationTaskData(dpu.dataUI)) {
      capsuleType = EXPLORATION_TASK 
      connectableWidget.addSampling
    } else capsuleType = BASIC_TASK
  }
}
