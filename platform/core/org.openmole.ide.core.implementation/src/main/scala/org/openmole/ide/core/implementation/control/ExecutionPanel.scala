/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.control

import java.awt.Dimension
import java.io.File
import javax.swing.filechooser.FileNameExtensionFilter
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.widget.CSVChooseFileTextField
import org.openmole.ide.core.implementation.widget.ChooseFileTextField
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import scala.swing.BoxPanel
import scala.swing.CheckBox
import scala.swing.Orientation
import scala.swing.Separator
import scala.swing.TextField

class ExecutionPanel(capsule: ICapsuleUI) extends BoxPanel(Orientation.Horizontal){
  val displayHookOption = new CheckBox("Display prototypes")
  val groupByJobOption = new CheckBox("Group by number of jobs: ")
  val groupByJobValue = new TextField{columns= 5;maximumSize = new Dimension(150,30)}
  
  contents += buildExecutionOptionPanel
  contents += new Separator
  contents += buildHookPanel
  contents += new Separator
  
  private def buildExecutionOptionPanel = new  BoxPanel(Orientation.Vertical){
    contents.append(new BoxPanel(Orientation.Horizontal) {contents.append(groupByJobOption,groupByJobValue)})
  }
    
  private def buildHookPanel = new BoxPanel(Orientation.Vertical){
    contents.append(displayHookOption)
    capsule.dataProxy.get.dataUI.prototypes.foreach( p=> {if(p.dataUI.coreObject.`type`.erasure == classOf[File]) contents.append(buildSaveFilePanel(p))})
  }
    
  private def buildSaveFilePanel(pdp: IPrototypeDataProxyUI) =  new BoxPanel(Orientation.Horizontal) {
    contents.append(new CheckBox("Save " + pdp.dataUI.name +" prototype in "),
                    new CSVChooseFileTextField(""))
  }
  
  
}
