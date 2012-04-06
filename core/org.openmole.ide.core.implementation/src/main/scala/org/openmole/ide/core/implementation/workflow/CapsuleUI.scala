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

import java.awt.Dimension
import java.awt.Point
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.implementation.data.CapsuleDataUI
import org.openmole.ide.core.implementation.dataproxy.ProxyFreezer
import org.openmole.ide.core.implementation.provider.CapsuleMenuProvider
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.workflow._
import org.openmole.ide.core.implementation.data.AbstractExplorationTaskDataUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.validation.DataflowProblem
import scala.swing.Action

class CapsuleUI(val scene: IMoleScene, 
                val dataUI : ICapsuleDataUI = new CapsuleDataUI) extends Widget(scene.graphScene) with ICapsuleUI{
  
  val taskComponentWidget = new TaskComponentWidget(scene,this,new TaskWidget(scene,this))
  var environmentWidget : Option[LinkedImageWidget] = None
  var samplingWidget : Option[LinkedImageWidget] = None
  var inputPrototypeWidget : Option[PrototypeWidget] = None
  var outputPrototypeWidget : Option[PrototypeWidget] = None
  setEnvironment(dataUI.environment)
  setSampling(dataUI.sampling)
  
  addChild(taskComponentWidget)
  setPreferredSize(new Dimension(TASK_CONTAINER_WIDTH+20,TASK_CONTAINER_HEIGHT+20))
  taskComponentWidget.setPreferredLocation(new Point(10,10))
  createActions(MOVE).addAction (ActionFactory.createMoveAction)
  
  
  var islots= ListBuffer.empty[IInputSlotWidget]
  val oslot= new OutputSlotWidget(scene,this)
  var nbInputSlots = 0
  
  addChild(oslot)
  val capsuleMenuProvider= new CapsuleMenuProvider(scene, this)
  
  getActions.addAction(ActionFactory.createPopupMenuAction(capsuleMenuProvider))
  
  scene.refresh
  def widget = this
  
  def copy(sc: IMoleScene) = {
    var slotMapping = new HashMap[IInputSlotWidget,IInputSlotWidget]
    val c = new CapsuleUI(sc)
    islots.foreach(i=>slotMapping+=i->c.addInputSlot(false))
    dataUI.task match {
      case Some(x : ITaskDataProxyUI) => 
        c.encapsule(ProxyFreezer.freeze(x))
        if(dataUI.environment.isDefined) c.setEnvironment(ProxyFreezer.freeze(dataUI.environment))
        if(dataUI.sampling.isDefined) c.setSampling(ProxyFreezer.freeze(dataUI.sampling))
      case _=>
    }
    (c,slotMapping)
  }
  
  def defineAsStartingCapsule(b : Boolean) = {
    dataUI.startingCapsule = b
    islots.foreach{ isw=>
      isw.setStartingSlot(b)}
    scene.validate
    scene.refresh
  }
  
  def decapsule = {
    dataUI.task = None
    removeChild(inputPrototypeWidget.get)
    removeChild(outputPrototypeWidget.get)
    inputPrototypeWidget = None
    outputPrototypeWidget = None
  }
  
  def encapsule(dpu: ITaskDataProxyUI)= {
    setTask(dpu)
    inputPrototypeWidget = Some(PrototypeWidget.buildInput(scene, dpu))
    outputPrototypeWidget = Some(PrototypeWidget.buildOutput(scene, dpu))
    addChild(inputPrototypeWidget.get)
    addChild(outputPrototypeWidget.get)
    capsuleMenuProvider.addTaskMenus
  }
  
  def setEnvironment(envtask : Option[IEnvironmentDataProxyUI]) = {
    dataUI.environment = envtask
    updateEnvironmentWidget
  }
    
  private def updateEnvironmentWidget = {
    environmentWidget match {
      case Some(y : LinkedImageWidget) => removeChild(y)
      case None =>
    }
    dataUI.environment match {
      case Some(x : IEnvironmentDataProxyUI) => 
        environmentWidget = Some(new LinkedImageWidget(scene,x.dataUI.imagePath,TASK_CONTAINER_WIDTH - 10,TASK_CONTAINER_HEIGHT -3,
                                                       new Action("") {def apply = scene.displayPropertyPanel(x,EDIT)}))
        addChild(environmentWidget.get)
      case None => environmentWidget = None
    }
    scene.refresh
  }
  
  def setSampling(sampletask : Option[ISamplingDataProxyUI]) = {
    dataUI.sampling = sampletask
    updateSamplingWidget
  }
  
  private def updateSamplingWidget = {
    samplingWidget match {
      case Some(y : LinkedImageWidget) => removeChild(y)
      case None =>
    }
    dataUI.sampling match {
      case None=> samplingWidget = None
      case Some(x : ISamplingDataProxyUI) => 
        samplingWidget = Some(new LinkedImageWidget(scene,x.dataUI.imagePath,0,TASK_CONTAINER_HEIGHT - 3,
                                                    new Action("") {def apply = scene.displayPropertyPanel(x,EDIT)}))
        addChild(samplingWidget.get)
    }
    scene.refresh
  }
  
  def updateErrors(problems : List[(IPrototypeDataProxyUI,DataflowProblem)]) = {
    dataUI.task match {
      case Some(x : ITaskDataProxyUI) => 
        val (protoOut,protoIn) = problems.partition{case (proto,problem)=> x.dataUI.prototypesIn.contains(proto)}
        inputPrototypeWidget.get.updateErrors(protoIn.map{_._2}.mkString("\n"))
        outputPrototypeWidget.get.updateErrors(protoOut.map{_._2}.mkString("\n"))
      case _ =>
    }
  }
  
  def addInputSlot(on: Boolean): IInputSlotWidget =  {
    if (on) dataUI.startingCapsule = on
    nbInputSlots+= 1
    val im = new InputSlotWidget(scene,this,nbInputSlots,on)
    islots += im
    addChild(im)
    scene.refresh
    im
  }

  def removeInputSlot= {
    nbInputSlots-= 1
    val toBeRemoved = islots.tail.last
    removeChild(toBeRemoved.widget)
    islots-= toBeRemoved
  }
  
  def setTask(dpu: ITaskDataProxyUI)={
    dataUI.task= Some(dpu)
    dpu.dataUI match {
      case x : AbstractExplorationTaskDataUI => setSampling(x.sampling)
      case _=>
    }
  }
  
  def x = convertLocalToScene(getLocation).getX
  
  def y = convertLocalToScene(getLocation).getY
}
