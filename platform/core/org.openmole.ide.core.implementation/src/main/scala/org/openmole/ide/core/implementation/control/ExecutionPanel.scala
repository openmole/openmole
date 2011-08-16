/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.control

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.io.File
import javax.swing.JPanel
import javax.swing.border.Border
import javax.swing.border.LineBorder
import org.openmole.plugin.hook.display.ToStringHook
import org.openide.util.Lookup
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ide.core.implementation.widget.ChooseFileTextField
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.factory.IHookFactoryUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.swing.BoxPanel
import scala.swing.CheckBox
import scala.swing.Component
import scala.swing.FileChooser
import scala.swing.FlowPanel
import scala.swing.Label
import scala.swing.Orientation
import scala.swing.Panel
import scala.swing.ScrollPane
import scala.swing.Separator
import scala.swing.TextField
import scala.swing.Alignment
import java.awt.Font._
import scala.swing.event.ButtonClicked
import scala.collection.JavaConversions._

class ExecutionPanel(execution: IMoleExecution,prototypes: HashMap[IPrototypeDataProxyUI,IPrototype[_<:Any]],capsuleUI: ICapsuleUI,capsule: ICapsule) extends FlowPanel{
 
  yLayoutAlignment = 0.0F
  xLayoutAlignment = 0.0F
  val groupByJobOption = new CheckBox("Group by number of jobs: ")
  val groupByJobValue = new TextField{columns= 5;maximumSize = new Dimension(150,30)}
  
  contents += buildExecutionOptionPanel
  contents += new Separator
  contents += buildHookPanel
  contents += new Separator
  
  private def buildExecutionOptionPanel = 
    new  BoxPanel(Orientation.Vertical){
    contents += new Label("Execution options: "){xAlignment = Alignment.Left;font = new Font("Ubuntu", BOLD,font.getSize)}
    contents.append(new BoxPanel(Orientation.Horizontal) {contents.append(groupByJobOption,groupByJobValue)})
  }
  
  private def buildHookPanel = new FlowPanel {
    Lookup.getDefault.lookupAll(classOf[IHookFactoryUI]).foreach(f=>{peer.add(f.buildPanelUI(execution,prototypes,capsuleUI,capsule).peer)})}
  
}