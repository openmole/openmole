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
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.workflow.implementation.EntityUI
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.commons.Constants

class DnDTaskIntoCapsuleProvider(molescene: MoleScene,val capsuleView: ICapsuleView) extends DnDProvider(molescene) {
  var encapsulated= false
  
  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= {
    var state= ConnectorState.REJECT
    if (!encapsulated){
      if (transferable.isDataFlavorSupported(Constants.TASK_DATA_FLAVOR)) state = ConnectorState.ACCEPT
    }
    else {
      if (transferable.isDataFlavorSupported(Constants.PROTOTYPE_DATA_FLAVOR)) state = ConnectorState.ACCEPT
       
      else if (transferable.isDataFlavorSupported(Constants.SAMPLING_DATA_FLAVOR)){
        println("WIWI " + widget)
        widget match{
          case x: ICapsuleView => {
              println("NAMMEEE " + x.capsuleModel.taskUI.get.panelUIData.name)
              if (x.capsuleModel.taskUI.get.factoryUI.asInstanceOf[ITaskFactoryUI].isExplorationTask) state = ConnectorState.ACCEPT
              else state = ConnectorState.REJECT
            }
          case _=> state = ConnectorState.REJECT
        }
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
    else if (transferable.isDataFlavorSupported(Constants.PROTOTYPE_DATA_FLAVOR)){
      println("PROTO !!" )
      val proto = transferable.getTransferData(Constants.PROTOTYPE_DATA_FLAVOR).asInstanceOf[EntityUI]
      if (point.x < capsuleView.connectableWidget.widgetWidth / 2) {
        println("ADD IN ")
        capsuleView.capsuleModel.taskUI.get.addPrototype(proto, IOType.INPUT)
      } else {
        println("ADD OUT ")
        capsuleView.capsuleModel.taskUI.get.addPrototype(proto, IOType.OUTPUT)
      }
    }
    else if (transferable.isDataFlavorSupported(Constants.SAMPLING_DATA_FLAVOR)){
      println("SAMPLING !!" )
    }
  }
}