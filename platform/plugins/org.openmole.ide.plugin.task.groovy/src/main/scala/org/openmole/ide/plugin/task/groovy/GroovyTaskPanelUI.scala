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
import org.openmole.ide.misc.widget.ChooseFileTextField
import scala.swing.FileChooser.SelectionMode._
import org.openmole.ide.misc.widget.MigPanel
import scala.swing.Label
import scala.swing.ScrollPane
import scala.swing.Swing
import scala.swing.TextArea
import scala.swing.TextField

class GroovyTaskPanelUI(pud: GroovyTaskDataUI) extends MigPanel("fillx,wrap 2","[left][grow,fill]","") with ITaskPanelUI {
  
  val codeTextArea = new TextArea {text = pud.code}
  val lib= new ChooseFileTextField(pud.lib, "Select a file", Some("Lib files"), FilesOnly,Some("jar"))
  val importStatement= new TextField{text= pud.importStatement}
  
  contents += (new Label("Code"),"wrap")
  contents += (new ScrollPane(codeTextArea){minimumSize = new Dimension(150,150)},"span 2,growx")
  contents += new Label("Lib")
  contents += (lib,"growx")
  contents += new Label("Import")
  contents += importStatement
  override def saveContent(name: String):ITaskDataUI = new GroovyTaskDataUI(name,codeTextArea.text,lib.text,importStatement.text)
}

//  val editorPane= new JEditorPane
//    val kit= CloneableEditorSupport.getEditorKit("text/x-groovy")
//    editorPane.setEditorKit(kit)
//    val fob= FileUtil.createMemoryFileSystem().getRoot().createData("tmp","groovy")
//    val dob= DataObject.find(fob)
//    editorPane.getDocument.putProperty(Document.StreamDescriptionProperty, dob)
//    editorPane.setText("package dummy;")
//    
