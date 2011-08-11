/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.control

import java.awt.Rectangle
import java.io.OutputStream
import java.io.PrintStream
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import scala.swing.ScrollPane
import scala.swing.TabbedPane
import scala.collection.JavaConversions._
import scala.swing.TextArea

class ExecutionTabbedPane(manager: IMoleSceneManager) extends TabbedPane {
  val logTextArea = new TextArea{columns = 20;rows = 10}
  System.setOut(new PrintStream(new TextAreaOutputStream(logTextArea)))
  System.setErr(new PrintStream(new TextAreaOutputStream(logTextArea)))
  
  manager.capsules.values.foreach{c=>pages+= new TabbedPane.Page(c.dataProxy.get.dataUI.name,new ExecutionPanel(c))}
  pages+= new TabbedPane.Page("Log",new ScrollPane(logTextArea))
  
  
  class TextAreaOutputStream(textArea: TextArea) extends OutputStream {
    override def flush = textArea.repaint
    override def write(b:Int) = {
      textArea.append(b.asInstanceOf[Byte].toString)
      textArea.peer.scrollRectToVisible(new Rectangle(0, textArea.size.height - 2, 1, 1))
    }
  }
}
