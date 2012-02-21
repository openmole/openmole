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
package org.openmole.ide.plugin.task.systemexec

import org.openmole.ide.core.implementation.data.EmptyDataUIs.EmptyPrototypeDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import java.awt.Dimension
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiComboTextField
import org.openmole.ide.misc.widget.multirow.MultiTextFieldCombo
import scala.swing.FileChooser._
import scala.swing._
import swing.Swing._

class SystemExecTaskPanelUI(ndu: SystemExecTaskDataUI) extends PluginPanel("fillx,wrap 2", "[left][grow,fill]","") with ITaskPanelUI{
 
  val workdirTextField = new TextField(ndu.workdir)
  val resourcesMultiTextField = new MultiChooseFileTextField("Resource",ndu.resources,SelectionMode.FilesAndDirectories)
  val outputMapMultiTextFieldCombo = new MultiTextFieldCombo[IPrototypeDataProxyUI]("Output mapping",
                                                                                   ndu.outputMap,
                                                                                   comboContent)
   
  val inputMapMultiComboTextField = new MultiComboTextField[IPrototypeDataProxyUI]("Input mapping",
                                                                                   ndu.inputMap,
                                                                                   comboContent)                           
  val launchingCommandTextArea = new TextArea(ndu.lauchingCommands) 
  
  contents+= new Label("Workdir")
  contents+= (workdirTextField,"growx,span 3, wrap")
  contents+= (new Label("Commands"),"wrap")
  contents+= (new ScrollPane(launchingCommandTextArea){minimumSize = new Dimension(150,80)},"span 2,growx")
  contents+= (resourcesMultiTextField.panel,"span 2, growx, wrap")
  contents+= (inputMapMultiComboTextField.panel,"span,grow,wrap")
  contents+= (outputMapMultiTextFieldCombo.panel,"span,grow,wrap")
  
  override def saveContent(name: String): ITaskDataUI = new SystemExecTaskDataUI(name,
                                                                                 workdirTextField.text, 
                                                                                 launchingCommandTextArea.text,
                                                                                 resourcesMultiTextField.content,
                                                                                 inputMapMultiComboTextField.content,
                                                                                 outputMapMultiTextFieldCombo.content)
  
  def comboContent: List[IPrototypeDataProxyUI] = new PrototypeDataProxyUI(new EmptyPrototypeDataUI)::Proxys.filePrototypes
}
