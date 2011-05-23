/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.provider

import java.awt.Point
import java.awt.datatransfer.Transferable
import org.netbeans.api.visual.widget.Widget
import org.netbeans.api.visual.action.ConnectorState
import org.openmole.ide.core.workflow.model.ICapsuleView
import org.openmole.ide.core.workflow.implementation.TaskUI
import org.openmole.ide.core.properties.ExplorationPanelUIData
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.workflow.implementation.EntityUI
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.workflow.implementation.paint.SamplingWidget
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.ide.core.commons.Constants

class DnDTaskIntoCapsuleProvider(molescene: MoleScene,val capsuleView: ICapsuleView) extends DnDProvider(molescene) {
  var encapsulated= false
  
  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= {
    
    var state= ConnectorState.REJECT
    if (!encapsulated){
      if (transferable.isDataFlavorSupported(Constants.TASK_DATA_FLAVOR)) state = ConnectorState.ACCEPT
    }
    else if (transferable.isDataFlavorSupported(Constants.ENTITY_DATA_FLAVOR)){
      transferable.getTransferData(Constants.ENTITY_DATA_FLAVOR).asInstanceOf[EntityUI].entityType match {
        case Constants.PROTOTYPE=> state = ConnectorState.ACCEPT
        case Constants.SAMPLING=> if (ElementFactories.isExplorationTaskFactory(capsuleView.capsuleModel.taskUI.get.factoryUI)) state = ConnectorState.ACCEPT
        case Constants.ENVIRONMENT=> println("envir"); state = ConnectorState.ACCEPT
        case _=> throw new GUIUserBadDataError("Unknown entity type")
      }
    }
    state
  }
  
  override def accept(widget: Widget,point: Point,transferable: Transferable)= {  
    if (transferable.isDataFlavorSupported(Constants.TASK_DATA_FLAVOR)){
      println("TASK !!" )
      capsuleView.encapsule(transferable.getTransferData(Constants.TASK_DATA_FLAVOR).asInstanceOf[TaskUI])
      capsuleView.addInputSlot
      molescene.repaint
      molescene.revalidate
    }
    else if (transferable.isDataFlavorSupported(Constants.ENTITY_DATA_FLAVOR)){
      println("ENTITY !!" )
      val entity = transferable.getTransferData(Constants.ENTITY_DATA_FLAVOR).asInstanceOf[EntityUI]
      entity.entityType match{
        case Constants.PROTOTYPE=> { 
            if (point.x < capsuleView.connectableWidget.widgetWidth / 2) {
              println("ADD IN ")
              capsuleView.capsuleModel.taskUI.get.addPrototype(entity, IOType.INPUT)
            } else {
              println("ADD OUT ")
              capsuleView.capsuleModel.taskUI.get.addPrototype(entity, IOType.OUTPUT)
            }
          }
        case Constants.SAMPLING=> {
            capsuleView.capsuleModel.taskUI.get.panelUIData.asInstanceOf[ExplorationPanelUIData].sampling = Some(entity)
            capsuleView.connectableWidget.addSampling(new SamplingWidget(molescene,entity.factoryUI)) 
        }
      }
    }
  }
}