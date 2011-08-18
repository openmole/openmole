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

import javax.swing.filechooser.FileNameExtensionFilter
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.misc.widget.ChooseFileTextField
import java.awt.Dimension
import scala.swing._
import swing.Swing._

class SystemExecTaskPanelUI(ndu: SystemExecTaskDataUI) extends BoxPanel(Orientation.Vertical) with ITaskPanelUI{
 
  border = Swing.EmptyBorder(10, 10, 10, 10)
  val workspaceTextField = new ChooseFileTextField(ndu.workspace){preferredSize = new Dimension(100,25)}
  val launchingCommandTextArea = new TextArea(ndu.lauchingCommands) 
  
  contents+= new BoxPanel(Orientation.Horizontal){contents.append(buildLabel("Workspace: "),workspaceTextField)}
  contents+= new BoxPanel(Orientation.Vertical){contents.append(buildLabel("Commands: "),new ScrollPane(launchingCommandTextArea)); border = Swing.EmptyBorder(10,10,10,10);preferredSize = new Dimension(130,25)}
  
  
  override def saveContent(name: String): ITaskDataUI = new SystemExecTaskDataUI(name, workspaceTextField.text, launchingCommandTextArea.text)
  
  def buildLabel(labelS: String) =  new Label(labelS) {
        border = Swing.EmptyBorder(5,5,5,5)}
}
