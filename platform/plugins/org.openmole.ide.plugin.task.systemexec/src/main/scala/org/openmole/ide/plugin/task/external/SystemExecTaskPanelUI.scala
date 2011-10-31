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
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import java.awt.Dimension
import org.openmole.ide.misc.widget.MigPanel
import scala.swing._
import swing.Swing._

class SystemExecTaskPanelUI(ndu: SystemExecTaskDataUI) extends MigPanel("fillx,wrap 4", "[][grow,fill][][grow,fill]","") with ITaskPanelUI{
 
  val workspaceTextField = new ChooseFileTextField(ndu.workspace)
  val resourcesTextField = new MultiChooseFileTextField("Resource",ndu.resources)
  val launchingCommandTextArea = new TextArea(ndu.lauchingCommands) 
  
  contents+= new Label("Workspace")
  contents+= (workspaceTextField,"growx,span 3, wrap")
  contents+= (new Label("Commands"),"wrap")
  contents+= (new ScrollPane(launchingCommandTextArea){minimumSize = new Dimension(150,200)},"span 4,growx")
  contents+= (resourcesTextField,"growx,span 3, wrap")
  
  override def saveContent(name: String): ITaskDataUI = new SystemExecTaskDataUI(name,
                                                                                 "", 
                                                                                 launchingCommandTextArea.text,
                                                                                 resourcesTextField.content.flatMap(_.map(_._2)))
}
