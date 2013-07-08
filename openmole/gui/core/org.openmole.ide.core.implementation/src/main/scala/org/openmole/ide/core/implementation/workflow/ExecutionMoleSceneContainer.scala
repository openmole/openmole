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

import java.awt.{ BorderLayout, Color }
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants._
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.ide.core.implementation.execution.ExecutionManager
import org.openmole.ide.core.implementation.execution.MoleFinishedEvent
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget._
import scala.swing._
import org.openmole.ide.core.implementation.registry.{ DefaultKey, KeyRegistry }
import org.openmole.ide.core.implementation.builder.MoleFactory
import util.{ Failure, Success }

class ExecutionMoleSceneContainer(val scene: ExecutionMoleScene,
                                  val page: TabbedPane.Page,
                                  val bmsc: BuildMoleSceneContainer) extends Panel with ISceneContainer {
  peer.setLayout(new BorderLayout)

  val executionManager =
    MoleFactory.buildMole(bmsc.scene.dataUI) match {
      case Success((mole, prototypeMapping, capsuleMapping, errors)) ⇒
        Some(new ExecutionManager(bmsc.scene.dataUI,
          this,
          mole,
          prototypeMapping,
          capsuleMapping))
      case Failure(l) ⇒ None
    }

  val startStopButton = new Button(start) {
    background = new Color(125, 160, 0)
  }

  /* val exportButton = new Button(export) {
    background = new Color(55, 170, 200)
  }*/

  val dlLabel = new Label("0/0")
  val ulLabel = new Label("0/0")

  executionManager match {
    case Some(eManager: ExecutionManager) ⇒
      eManager.capsuleMapping.keys.foreach {
        c ⇒
          c.dataUI.task match {
            case Some(t: ITaskDataProxyUI) ⇒
            case _                         ⇒
          }
      }

      val executionPanel = new ExecutionPanel
      executionPanel.peer.setLayout(new BorderLayout)
      executionPanel.peer.setLayout(new BorderLayout)

      peer.add(new PluginPanel("wrap") {
        contents += new TitleLabel("Execution control")
        contents += new PluginPanel("wrap 2", "[]-20[]5[]") {
          contents += startStopButton
          // contents += exportButton
          contents += new PluginPanel("wrap 4") {
            contents += new Label("Downloads:")
            contents += dlLabel
            contents += new Label("Uploads:")
            contents += ulLabel
          }

          // View Mole execution
          contents += new MainLinkLabel("Mole execution", new Action("") {
            def apply =
              DialogDisplayer.getDefault.notify(new DialogDescriptor(new JScrollPane(scene.graphScene.createView) {
                setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED)
              }, "Mole execution"))
          })
        }
        contents += eManager.envBarPanel
      }.peer, BorderLayout.NORTH)

      peer.add(eManager.peer)

    case None ⇒
  }

  def start: Action = new Action("Start") {
    override def apply = executionManager match {
      case Some(x: ExecutionManager) ⇒
        listenTo(x)
        reactions += {
          case MoleFinishedEvent ⇒
            startLook
        }
        startStopButton.background = new Color(170, 0, 0)
        startStopButton.action = new Action("Stop") { def apply = stop }
        //exportButton.enabled = false
        x.start
      case _ ⇒
    }
  }

  def startLook = {
    startStopButton.background = new Color(125, 160, 0)
    startStopButton.action = start
    // exportButton.enabled = true
  }

  def stop = executionManager match {
    case Some(x: ExecutionManager) ⇒
      startStopButton.background = new Color(125, 160, 0)
      startStopButton.action = start
      x.cancel
    case _ ⇒
  }

  def updateFileTransferLabels(dl: String, ul: String) = {
    dlLabel.text = dl
    ulLabel.text = ul
    revalidate
  }

  def moleExecution = executionManager match {
    case Some(x: ExecutionManager) ⇒ x.moleExecution
    case _                         ⇒ None
  }

  def started = moleExecution match {
    case Some(me: MoleExecution) ⇒ me.started
    case _                       ⇒ false
  }

  def finished = moleExecution match {
    case Some(me: MoleExecution) ⇒ me.finished
    case _                       ⇒ false
  }

  class ExecutionPanel extends Panel {
    background = new Color(77, 77, 77)
  }

}
