/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.provider

import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.commons.ApplicationCustomize
import org.netbeans.api.visual.action.AcceptProvider
import org.netbeans.api.visual.action.ConnectorState
import org.netbeans.api.visual.widget.Widget
import java.awt.datatransfer.Transferable
import java.awt.Point

class DnDNewEntityProvider extends AcceptProvider{

  override def isAcceptable(widget: Widget,point: Point,transferable: Transferable): ConnectorState=  {
    var state= ConnectorState.REJECT
    if (transferable.isDataFlavorSupported(ApplicationCustomize.PROTOTYPE_DATA_FLAVOR) ||
    transferable.isDataFlavorSupported(ApplicationCustomize.SAMPLING_DATA_FLAVOR)||
    transferable.isDataFlavorSupported(ApplicationCustomize.TASK_DATA_FLAVOR)) state = ConnectorState.ACCEPT
    state
  }
  
  override def accept(widget: Widget,point: Point,t: Transferable) = System.out.println("ACCEPT")
}
