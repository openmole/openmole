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
import javax.swing.{ JSplitPane, JScrollPane }
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.misc.widget.ToolBarButton
import scala.collection.mutable.HashSet
import swing._
import org.openmole.ide.misc.tools.image.Images._
import java.awt.Toolkit
import org.openmole.ide.core.implementation.dialog.{ MoleSettingsDialog, StatusBar }

class BuildMoleSceneContainer(val scene: BuildMoleScene) extends Panel with ISceneContainer {
  buildContainer ⇒

  val executionMoleSceneContainers = new HashSet[ExecutionMoleSceneContainer]
  val statusBar = new StatusBar

  peer.setLayout(new BorderLayout)

  val toolBar = new MigPanel("") {

    contents += new ToolBarButton(MOLE_SETTINGS,
      "Mole settings",
      displayMoleSettingsDialogAction)

    contents += new ToolBarButton(BUILD_EXECUTION,
      "Build the workflow",
      buildExecutionAction)

    contents += new ToolBarButton(CLEAN_BUILD_EXECUTION,
      "Clean and build the workflow",
      cleanAndBuildExecutionAction)
  }

  peer.add(toolBar.peer, BorderLayout.NORTH)

  val view = scene.graphScene.createView
  view.setFocusable(true)

  val spane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(view), statusBar.peer)
  spane.setResizeWeight(1 - (50.0 / Toolkit.getDefaultToolkit.getScreenSize.height))
  peer.add(spane, BorderLayout.CENTER)
  CheckData.checkMole(scene, false)

  def stopAndCloseExecutions = {
    executionMoleSceneContainers.foreach {
      emc ⇒
        emc.stop
        ScenesManager.tabPane.pages.remove(emc.page.index)
    }
    executionMoleSceneContainers.clear
  }

  def buildExecutionAction = new Action("") {
    override def apply = {
      ScenesManager.saveCurrentPropertyWidget
      CheckData.checkNoEmptyCapsule(scene)
      ScenesManager.addExecutionSceneContainer(buildContainer)
    }
  }

  def cleanAndBuildExecutionAction = new Action("") {
    override def apply = {
      stopAndCloseExecutions
      ScenesManager.addExecutionSceneContainer(buildContainer)
    }
  }

  def displayMoleSettingsDialogAction = new Action("") {
    override def apply = {
      MoleSettingsDialog.display(scene.manager)
    }
  }
}
