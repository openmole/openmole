/*
 * Copyright (C) 2011 leclaire
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.dialog

import java.awt.Color
import java.awt.Dimension
import javax.swing.JOptionPane
import javax.swing.JOptionPane._
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.implementation.control.ExecutionMoleComponent
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.model.control.IMoleComponent
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.misc.widget.GroovyEditor
import scala.swing.Label
import scala.swing.TextField

object DialogFactory {
  
  def closeExecutionTab(mc: IMoleComponent): Boolean = { 
    mc match {
      case x: ExecutionMoleComponent=> 
        if (x.executionManager.moleExecution.finished) true
        else if (x.executionManager.moleExecution.started){
          val lab = new Label("<html>A simulation is currently running.<br>Close anyway ?</html>"){
            background = Color.white}.peer
          if (DialogDisplayer.getDefault.notify(new DialogDescriptor(lab, "Execution warning")).equals(NotifyDescriptor.OK_OPTION)) true
          else false 
        }
        else true
    }
  }
  
  def newTabName : Option[MoleSceneTopComponent]= { 
    val textField = new TextField("Mole_" + (TopComponentsManager.topComponents.size + 1),20)
    if (DialogDisplayer.getDefault.notify(new DialogDescriptor(textField.peer, "Mole name")).equals(NotifyDescriptor.OK_OPTION))
      Some(TopComponentsManager.addTopComponent(textField.text))
    else None
  }
  
  def deleteProxyConfirmation(proxy : IDataProxyUI) : Boolean = {
    if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new Label("<html>" +proxy.dataUI.name + " is currently used in a scene.<br>" +
                                                                         "It will be deleted everywher it appears. <br>" +
                                                                         "Delete anyway ?").peer, "Execution warning")).equals(NotifyDescriptor.OK_OPTION)) true
    else false 
  }
}
