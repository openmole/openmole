/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.control

import java.awt.Rectangle
import java.io.OutputStream
import java.io.PrintStream
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import scala.swing.ScrollPane
import scala.swing.TabbedPane
import scala.collection.JavaConversions._
import scala.swing.TextArea

class ExecutionTabbedPane(manager: IMoleSceneManager) extends TabbedPane {
  val logTextArea = new TextArea{columns = 20;rows = 10}
  val (execution, prototypes,capsules) = MoleMaker.buildMoleExecution(manager)
  
  System.setOut(new PrintStream(new TextAreaOutputStream(logTextArea)))
  System.setErr(new PrintStream(new TextAreaOutputStream(logTextArea)))
  
  capsules.keys.foreach{c=>pages+= new TabbedPane.Page(c.dataProxy.get.dataUI.name,new ExecutionPanel(execution,prototypes,c,capsules(c)))}
  pages+= new TabbedPane.Page("Log",new ScrollPane(logTextArea))
  
  def start = execution.start
  
  class TextAreaOutputStream(textArea: TextArea) extends OutputStream {
    override def flush = textArea.repaint
    override def write(b:Int) = {
      textArea.append(new String(Array[Byte](b.asInstanceOf[Byte])))
      textArea.peer.scrollRectToVisible(new Rectangle(0, textArea.size.height - 2, 1, 1))
    }
  }
}
