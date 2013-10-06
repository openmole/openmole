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
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants._
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.ide.core.implementation.execution.ExecutionManager
import org.openmole.ide.core.implementation.execution.MoleFinishedEvent
import org.openmole.ide.misc.widget._
import scala.swing._
import org.openmole.ide.core.implementation.builder.MoleFactory
import util.{ Failure, Success }
import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyUI
import scala.swing.event.ButtonClicked
import org.openmole.ide.core.implementation.preference.{ SandBox, ServerListPanel }
import java.net.URL
import org.openmole.misc.workspace.Workspace
import java.io.File
import org.openmole.core.model.mole.ExecutionContext

class ExecutionMoleSceneContainer(val scene: ExecutionMoleScene,
                                  val page: TabbedPane.Page,
                                  val bmsc: BuildMoleSceneContainer) extends Panel with ISceneContainer {
  peer.setLayout(new BorderLayout)

  val executionManager =
    MoleFactory.buildMole(bmsc.scene.dataUI) match {
      case Success((mole, capsuleMapping, errors)) ⇒
        Some(new ExecutionManager(bmsc.scene.dataUI,
          this,
          mole,
          capsuleMapping))
      case Failure(l) ⇒ None
    }

  val startStopButton = new Button(start) {
    background = new Color(125, 160, 0)
  }

  val dlLabel = new Label("0/0")
  val ulLabel = new Label("0/0")
  val serverCheckBox = new CheckBox("Delegates to : ")
  val serverCombo = new ComboBox(ServerListPanel.list)
  serverCombo.enabled = false

  val sandBoxCheckBox = new CheckBox("Sandbox")
  val sandBoxTextField: ChooseFileTextField = new ChooseFileTextField(SandBox.apply, Workspace.persistent("gui").save(sandBoxTextField.text, "sandbox")
  )
  sandBoxTextField.enabled = false

  executionManager match {
    case Some(eManager: ExecutionManager) ⇒
      eManager.capsuleMapping.keys.foreach {
        c ⇒
          c.dataUI.task match {
            case Some(t: TaskDataProxyUI) ⇒
            case _                        ⇒
          }
      }

      val executionPanel = new ExecutionPanel
      executionPanel.peer.setLayout(new BorderLayout)
      executionPanel.peer.setLayout(new BorderLayout)

      peer.add(new PluginPanel("wrap") {
        contents += new TitleLabel("Execution control")
        contents += new PluginPanel("wrap 2", "[]-20[]5[]") {
          contents += startStopButton
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

        contents += new PluginPanel("wrap 2") {
          contents += serverCheckBox
          contents += serverCombo
        }

        contents += new PluginPanel("wrap 3") {
          contents += sandBoxCheckBox
          contents += sandBoxTextField
          contents += new Label("<html><i>The sandbox folder is a root folder from which all paths are appended; allowing portability of the workflows." +
            "<br/>Ex: sandbox folder = /tmp/ and a Copy File hook is set to /home/mole/; files will be copied in /tmp/home/mole." +
            "<br/>It is the default mode when the computation is delegated to a server.</i></html>")
        }

        listenTo(`serverCheckBox`, `sandBoxCheckBox`)
        reactions += {
          case ButtonClicked(`serverCheckBox`) ⇒
            serverCombo.enabled = serverCheckBox.selected
            sandBoxTextField.enabled = !serverCombo.enabled
            sandBoxCheckBox.enabled = sandBoxCheckBox.enabled
          case ButtonClicked(`sandBoxCheckBox`) ⇒
            sandBoxTextField.enabled = sandBoxCheckBox.selected
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
        startStopButton.action = new Action("Stop") {
          def apply = stop
        }
        x.start({
          if (serverCheckBox.selected) Some(new URL(serverCombo.selection.item))
          else None
        }, {
          if (sandBoxCheckBox.selected) ExecutionContext.local.copy(directory = Some(new File(sandBoxTextField.text)))
          else ExecutionContext.local
        })
      case _ ⇒
    }
  }

  def startLook = {
    startStopButton.background = new Color(125, 160, 0)
    startStopButton.action = start
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
