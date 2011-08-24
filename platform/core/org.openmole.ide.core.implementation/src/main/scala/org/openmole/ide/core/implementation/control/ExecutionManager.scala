/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.control

import java.awt.Rectangle
import java.io.OutputStream
import java.io.PrintStream
import org.openide.util.Lookup
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.model.hook.IHook
import org.openmole.ide.misc.widget.MigPanel
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
import scala.collection.JavaConversions._
import scala.swing.TextArea

class ExecutionManager(manager : IMoleSceneManager) extends SplitPane(Orientation.Vertical) with IExecutionManager{
  val tabbedPane = new TabbedPane
  val logTextArea = new TextArea{columns = 20;rows = 10}
  override val printStream = new PrintStream(new TextAreaOutputStream(logTextArea))
  override val (mole, prototypeMapping,capsuleMapping) = MoleMaker.buildMole(manager)
  var moleExecution = new MoleExecution(mole)
  var hookPanels= new HashMap[IHookPanelUI,IHook]
  
  System.setOut(new PrintStream(new TextAreaOutputStream(logTextArea)))
  System.setErr(new PrintStream(new TextAreaOutputStream(logTextArea)))
  
  Lookup.getDefault.lookupAll(classOf[IHookFactoryUI]).foreach(hf=>{
      val dataUI = hf.buildDataUI(this)
      val panelUI = dataUI.buildPanelUI
      tabbedPane.pages+= new TabbedPane.Page(hf.displayName,new MigPanel(""){peer.add(panelUI.peer)})})
  
  leftComponent = new ScrollPane(tabbedPane)
  rightComponent = new ScrollPane(logTextArea)
  
  def start = {
    cancel
    moleExecution = new MoleExecution(mole)
    commitHooks
    moleExecution.start}
  
  def cancel = moleExecution.cancel
  
  override def commitHook(hookPanelUI: IHookPanelUI) {
    val hookObject = hookPanelUI.saveContent.coreObject
    hookPanels.getOrElseUpdate(hookPanelUI,hookObject).release
    hookPanels(hookPanelUI) = hookObject
    hookObject.resume
  }
    
  def commitHooks = hookPanels.keys.foreach(commitHook(_))
  
  class TextAreaOutputStream(textArea: TextArea) extends OutputStream {
    override def flush = textArea.repaint
    override def write(b:Int) = {
      textArea.append(new String(Array[Byte](b.asInstanceOf[Byte])))
      textArea.peer.scrollRectToVisible(new Rectangle(0, textArea.size.height - 2, 1, 1))
    }
  }
}
