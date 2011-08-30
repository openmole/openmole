/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.control

import java.awt.Rectangle
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.io.PrintStream
import org.openide.util.Lookup
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.model.hook.IHook
import org.openmole.ide.misc.visualization.PiePlotter
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.model.factory.IHookFactoryUI
import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.ide.core.model.panel.IHookPanelUI
import scala.collection.mutable.HashMap
import scala.swing.Orientation
import scala.swing.ScrollPane
import scala.swing.SplitPane
import scala.swing.TabbedPane
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.Priority
import scala.collection.JavaConversions._
import scala.swing.TextArea
import org.openmole.core.model.job.State._

class ExecutionManager(manager : IMoleSceneManager) extends SplitPane(Orientation.Vertical) with IExecutionManager{
  val tabbedPane = new TabbedPane
  val logTextArea = new TextArea{columns = 20;rows = 10}
  override val printStream = new PrintStream(new BufferedOutputStream(new TextAreaOutputStream(logTextArea),1024),true)
  override val (mole, prototypeMapping,capsuleMapping) = MoleMaker.buildMole(manager)
  var moleExecution: IMoleExecution = new MoleExecution(mole)
  var hookPanels= new HashMap[IHookPanelUI,IHook]
  var status = new HashMap[State,Int]
  status+= READY-> 0
  status+= RUNNING-> 0
  status+= COMPLETED-> 0
  status+= FAILED-> 0
  status+= CANCELED-> 0
  
  System.setOut(new PrintStream(new BufferedOutputStream(new TextAreaOutputStream(logTextArea)),true))
  System.setErr(new PrintStream(new BufferedOutputStream(new TextAreaOutputStream(logTextArea)),true))
  
  Lookup.getDefault.lookupAll(classOf[IHookFactoryUI]).foreach(hf=>{
      val dataUI = hf.buildDataUI(this)
      val panelUI = dataUI.buildPanelUI
      tabbedPane.pages+= new TabbedPane.Page(hf.displayName,new MigPanel(""){peer.add(panelUI.peer)})})
  
  val wfPiePlotter = new PiePlotter("Workflow",Map("Ready"-> 0.0,"Submitted"-> 0.0,"Running"-> 0.0,"Done"-> 0.0,"Failed"-> 0.0,"Killed"-> 100.0))
 // val curPiePlotter = new PiePlotter("Current Environment",Map("Ready"-> 0.0,"Submitted"-> 0.0,"Running"-> 0.0,"Done"-> 0.0,"Failed"-> 0.0,"Killed"-> 0.0))
  tabbedPane.pages+= new TabbedPane.Page("Execution progress", new MigPanel("wrap 2"){
      peer.add(wfPiePlotter.chartPanel)})
    //  peer.add(curPiePlotter.chartPanel)
  
  leftComponent = new ScrollPane(tabbedPane)
  rightComponent = new ScrollPane(logTextArea)
  
  def start = {
    cancel
    hookPanels.values.foreach(_.release)
    moleExecution = MoleMaker.buildMoleExecution(mole, manager)
    EventDispatcher.registerForObjectChanged(moleExecution,Priority.NORMAL,new JobCreatedListener,IMoleExecution.OneJobSubmitted)
    hookPanels.keys.foreach(commitHook(_))
    moleExecution.start}
  
  def cancel = moleExecution.cancel
  
  override def commitHook(hookPanelUI: IHookPanelUI) {
    if (hookPanels.contains(hookPanelUI)) {println("realase");hookPanels(hookPanelUI).release}
    hookPanels+= hookPanelUI-> hookPanelUI.saveContent.coreObject
  }
  
  class TextAreaOutputStream(textArea: TextArea) extends OutputStream {
    override def flush = textArea.repaint
    
    override def write(b:Int) = textArea.append(new String(Array[Byte](b.asInstanceOf[Byte])))
                      
    override def write(b: Array[Byte], off: Int,len: Int) = {
      textArea.append(new String(b,off,len))
      textArea.peer.scrollRectToVisible(new Rectangle(0, textArea.size.height - 2, 1, 1))
    }
  }
}
