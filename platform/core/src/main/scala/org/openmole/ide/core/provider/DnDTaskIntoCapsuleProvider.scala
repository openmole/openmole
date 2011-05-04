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
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.workflow.implementation.TaskUI
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.palette.PaletteElementFactory

object DnDTaskIntoCapsuleProvider {

}

class DnDTaskIntoCapsuleProvider(molescene: MoleScene,val capsuleView: ICapsuleView) extends DnDProvider(molescene) {

  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= ConnectorState.ACCEPT
 
  override def accept(widget: Widget,point: Point,transferable: Transferable)= {
  //  capsuleView.encapsule(transferable.getTransferData(Constants.TASK_DATA_FLAVOR).asInstanceOf[ITaskFactoryUI].buildEntity)
  
    if (transferable.isDataFlavorSupported(Constants.PROTOTYPE_DATA_FLAVOR) ) println("PROTOTYPE_DATA_FLAVOR")
    if (transferable.isDataFlavorSupported(Constants.PROTOTYPE_MODEL_DATA_FLAVOR) ) println("PROTOTYPE_MODEL_DATA_FLAVOR")
    
    
    capsuleView.encapsule(transferable.getTransferData(Constants.TASK_DATA_FLAVOR).asInstanceOf[PaletteElementFactory].buildEntity.asInstanceOf[TaskUI])
    molescene.repaint
    molescene.revalidate
  }
}