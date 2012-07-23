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
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants._
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openmole.ide.core.implementation.execution.ExecutionManager
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget.MainLinkLabel
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.ToolBarButton
import scala.swing.Action
import scala.swing.Panel
import scala.swing.TabbedPane
import scala.swing.ScrollPane
import org.openmole.ide.misc.tools.image.Images._

class ExecutionMoleSceneContainer(val scene: ExecutionMoleScene,
                                  val page: TabbedPane.Page,
                                  bmsc: BuildMoleSceneContainer) extends Panel with ISceneContainer {

  peer.setLayout(new BorderLayout)
  val toolBar = new MigPanel("") {
    contents += new ToolBarButton(new ImageIcon(START_EXECUTION),
      "Start the workflow",
      start)

    contents += new ToolBarButton(new ImageIcon(STOP_EXECUTION),
      "Stop the workflow",
      stop)
  }

  val executionManager =
    MoleMaker.buildMole(bmsc.scene.manager) match {
      case Right((mole, prototypeMapping, capsuleMapping, errors)) ⇒
        Some(new ExecutionManager(bmsc.scene.manager,
          mole,
          prototypeMapping,
          capsuleMapping))
      case Left(l) ⇒
    }

  executionManager match {
    case Some(x: ExecutionManager) ⇒
      peer.add(toolBar.peer, BorderLayout.NORTH)

      peer.add(new PluginPanel("wrap") {
        contents += new MainLinkLabel("Mole execution", new Action("") {
          def apply =
            DialogDisplayer.getDefault.notify(new DialogDescriptor(new JScrollPane(scene.graphScene.createView) {
              setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED)
            }, "Mole execution"))
        })
      }.peer, BorderLayout.CENTER)
      peer.add(new PluginPanel("wrap", "[grow]") {
        add(x, "growx")
      }.peer, BorderLayout.SOUTH)
    case None ⇒
  }

  def start = new Action("") {
    override def apply = executionManager match {
      case Some(x: ExecutionManager) ⇒ x.start
      case _ ⇒
    }
  }

  def stop = new Action("") {
    override def apply = executionManager match {
      case Some(x: ExecutionManager) ⇒ x.cancel
      case _ ⇒
    }
  }

  def started = executionManager match {
    case Some(x: ExecutionManager) ⇒ x.moleExecution.started
    case None ⇒ false
  }

  def finished = executionManager match {
    case Some(x: ExecutionManager) ⇒ x.moleExecution.finished
    case None ⇒ true
  }
}