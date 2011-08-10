/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.control

import java.io.File
import javax.swing.filechooser.FileNameExtensionFilter
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.widget.CSVChooseFileTextField
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import scala.swing.BoxPanel
import scala.swing.CheckBox
import scala.swing.Orientation

class ExecutionPanel(capsule: ICapsuleUI) extends BoxPanel(Orientation.Vertical){
  contents.append(new CheckBox("Display prototypes"))
  
  capsule.dataProxy.get.dataUI.prototypes.foreach(p=>
    if (p.dataUI.coreClass.isAssignableFrom(classOf[IPrototype[File]])) contents.append(buildSaveFilePanel(p)))
  
  def buildSaveFilePanel(pdp: IPrototypeDataProxyUI) =  new BoxPanel(Orientation.Horizontal) {
    contents.append(new CheckBox("Save " + pdp.dataUI.name +" prototype in "),
                    new CSVChooseFileTextField)
  }
}
