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

package org.openmole.ide.core.implementation.control

import scala.swing.Orientation
import scala.swing.Button
import java.awt.Dimension
import javax.swing.ImageIcon
import org.openide.util.ImageUtilities
import org.openmole.ide.misc.widget.ToolBarButton
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.Priority
import org.openmole.misc.workspace.Workspace
import scala.swing.Action
import scala.swing.BoxPanel
import scala.swing.event.ButtonClicked
import org.openmole.ide.misc.widget.ToolBarButton

object ExecutionBoard extends BoxPanel(Orientation.Horizontal){
  val startButton = new ToolBarButton(new ImageIcon(ImageUtilities.loadImage("img/startExe.png")),"Start the workflow",
                                      new Action(""){override def apply = TopComponentsManager.currentExecutionManager.start})
                                       
  val stopButton = new ToolBarButton(new ImageIcon(ImageUtilities.loadImage("img/stopExe.png")),"Stop the workflow",
                                     new Action(""){override def apply =  TopComponentsManager.currentExecutionManager.cancel})
  contents.append(startButton,stopButton)
  
  EventDispatcher.listen(Workspace.instance, PasswordListener , classOf[Workspace.PasswordRequired])
  
  def activate(b:Boolean) = {
    startButton.visible = b
    stopButton.visible = b
  }
}
