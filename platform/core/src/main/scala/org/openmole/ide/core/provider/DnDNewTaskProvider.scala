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
import java.awt.datatransfer.Transferable
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.workflow.implementation.TaskUI
import org.netbeans.api.visual.action.ConnectorState
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.EntityPropertyTopComponent
import org.openmole.ide.core.MoleSceneTopComponent
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.workflow.implementation.TaskUI



class DnDNewTaskProvider(molescene: MoleScene) extends DnDProvider(molescene) {

  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= ConnectorState.ACCEPT
 
  override def accept(widget: Widget,point: Point,transferable: Transferable)= {
    println("+Accept ")
    println(transferable.isDataFlavorSupported(Constants.TASK_DATA_FLAVOR))
    
    val capsuleView = MoleScenesManager.createCapsule(molescene,point)
   // capsuleView.addInputSlot
    //  capsuleView.encapsule(transferable.getTransferData(Constants.TASK_DATA_FLAVOR).asInstanceOf[ITaskFactoryUI].buildEntity)
  
//    if (transferable.isDataFlavorSupported(Constants.TASK_MODEL_DATA_FLAVOR)){
//      println("++TASK_MODEL_DATA_FLAVOR")
//      val f = transferable.getTransferData(Constants.TASK_MODEL_DATA_FLAVOR).asInstanceOf[PaletteElementFactory]
//      
//      //val entity = f.buildNewEntity.asInstanceOf[TaskUI]
//      val entity = f.buildEntity
//      capsuleView.encapsule(entity.asInstanceOf[TaskUI])
//      ElementFactories.addElement(new PaletteElementFactory(entity.factoryUI.panelUIData.name,entity.factoryUI.imagePath,Constants.TASK,entity.factoryUI.getClass))
//      MoleSceneTopComponent.getDefault.refreshPalette
//      EntityPropertyTopComponent.getDefault.displayCurrentEntityPanel(entity.factoryUI)
//    }
    if (transferable.isDataFlavorSupported(Constants.TASK_DATA_FLAVOR))  {
      capsuleView.encapsule(transferable.getTransferData(Constants.TASK_DATA_FLAVOR).asInstanceOf[PaletteElementFactory].entity.asInstanceOf[TaskUI])
      println("++TASK_DATA_FLAVOR")
      
    }
  
    molescene.repaint
    molescene.revalidate
  }
}