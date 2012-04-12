/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.workflow

import java.awt.BorderLayout
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.execution.ExecutionManager
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.misc.widget.ToolBarButton
import scala.swing.Action
import scala.swing.Panel
import scala.swing.TabbedPane
import org.openmole.ide.misc.tools.image.Images._

class ExecutionMoleSceneContainer(val scene : ExecutionMoleScene,
                                  val page : TabbedPane.Page) extends Panel with ISceneContainer{
  
  peer.setLayout(new BorderLayout)
  val toolBar = new MigPanel("") {
    contents += new ToolBarButton(new ImageIcon(START_EXECUTION), 
                                  "Start the workflow",
                                  start)
    
    contents += new ToolBarButton(new ImageIcon(STOP_EXECUTION), 
                                  "Stop the workflow",
                                  stop)
  } 
  val (mole, prototypeMapping,capsuleMapping,errors) = MoleMaker.buildMole(scene.manager)
  val executionManager = new ExecutionManager(scene.manager,
                                              mole,
                                              prototypeMapping,
                                              capsuleMapping)
  
  peer.add(toolBar.peer,BorderLayout.NORTH)
  peer.add(scene.graphScene.createView,BorderLayout.CENTER)
  peer.add(executionManager.peer,BorderLayout.SOUTH)
  
  
  def start = new Action("") { override def apply = executionManager.start}
  
  def stop = new Action("") { override def apply = executionManager.cancel}
  
}