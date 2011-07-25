/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.task.groovy

import java.awt.Dimension
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import scala.swing.BoxPanel
import scala.swing.Label
import scala.swing.Orientation
import scala.swing.ScrollPane
import scala.swing.Swing
import scala.swing.TextArea

class GroovyTaskPanelUI(pud: GroovyTaskDataUI) extends BoxPanel(Orientation.Vertical) with ITaskPanelUI {
  
  contents += new Label("code: ")
  
val codeTextArea = new TextArea {
    preferredSize = new Dimension(60,25)
    border = Swing.EmptyBorder(5,5,5,5)
  }
  
  contents += new ScrollPane(codeTextArea)
  
  def saveContent(name: String): ITaskDataUI = new GroovyTaskDataUI(name,codeTextArea.text)
}

//  val editorPane= new JEditorPane
//    val kit= CloneableEditorSupport.getEditorKit("text/x-groovy")
//    editorPane.setEditorKit(kit)
//    val fob= FileUtil.createMemoryFileSystem().getRoot().createData("tmp","groovy")
//    val dob= DataObject.find(fob)
//    editorPane.getDocument.putProperty(Document.StreamDescriptionProperty, dob)
//    editorPane.setText("package dummy;")
//    
