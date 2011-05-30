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
import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.properties.IPanelUIData
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.properties.TaskPanelUIData
import org.openmole.ide.core.properties.PanelUIData
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.workflow.implementation.paint.SamplingWidget
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.ide.core.commons.Constants

class DnDTaskIntoCapsuleProvider(molescene: MoleScene,val capsuleView: ICapsuleView) extends DnDProvider(molescene) {
  var encapsulated= false
  
  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= {
    println("INTO CAPSULE ????????' " + transferable.getTransferData(Constants.ENTITY_DATA_FLAVOR).asInstanceOf[PaletteElementFactory].panelUIData.entityType)
    val ent = transferable.getTransferData(Constants.ENTITY_DATA_FLAVOR).asInstanceOf[PaletteElementFactory].panelUIData.entityType
    var state= ConnectorState.REJECT
    if (!encapsulated){
      if (ent == Constants.TASK) state = ConnectorState.ACCEPT
    }
    else {
      ent match {
        case Constants.PROTOTYPE=> state = ConnectorState.ACCEPT
        case Constants.SAMPLING=> if (ElementFactories.isExplorationTaskFactory(capsuleView.capsuleModel.dataProxy.get.panelUIData)) state = ConnectorState.ACCEPT
        case Constants.ENVIRONMENT=> println("envir"); state = ConnectorState.ACCEPT
        case _=> throw new GUIUserBadDataError("Unknown entity type")
      }
    }
    println("STATE :: " + state)
    state
  }
  
  override def accept(widget: Widget,point: Point,transferable: Transferable)= {  
    val pef = transferable.getTransferData(Constants.ENTITY_DATA_FLAVOR).asInstanceOf[PaletteElementFactory]
    val capsulePanelData = capsuleView.capsuleModel.dataProxy.get.panelUIData.asInstanceOf[TaskPanelUIData]
    pef.panelUIData.entityType match{
      case Constants.TASK=>{
          println("TASK !!" )
          capsuleView.encapsule(pef)
          capsuleView.addInputSlot
        }
      case Constants.PROTOTYPE=> { 
          println("PROTO !!" )
          if (point.x < capsuleView.connectableWidget.widgetWidth / 2) capsulePanelData.addPrototype(pef, IOType.INPUT)
          else capsulePanelData.addPrototype(pef, IOType.OUTPUT)
        }
      case Constants.SAMPLING=> capsulePanelData.sampling = Some(pef)
    }
    molescene.repaint
    molescene.revalidate
  }
}
