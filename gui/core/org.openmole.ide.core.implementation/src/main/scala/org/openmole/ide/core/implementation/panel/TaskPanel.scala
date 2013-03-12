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

package org.openmole.ide.core.implementation.panel

import java.awt.BorderLayout
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openide.awt.HtmlBrowser
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow._
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabel._
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor._
import scala.collection.mutable.HashMap
import scala.swing.Component
import scala.swing.Label
import scala.swing.TabbedPane
import swing.event.{ SelectionChanged, FocusGained }
import org.openmole.ide.core.implementation.prototype.UpdatedPrototypeEvent

class TaskPanel(proxy: ITaskDataProxyUI,
                scene: IMoleScene,
                mode: Value = CREATION) extends BasePanel(Some(proxy), scene, mode) {
  taskPanel ⇒
  iconLabel.icon = new ImageIcon(ImageIO.read(proxy.dataUI.getClass.getClassLoader.getResource(proxy.dataUI.imagePath)))

  val panelUI = proxy.dataUI.buildPanelUI
  panelUI.tabbedPane.pages.insert(1, new TabbedPane.Page("Inputs / Outputs", new Label))

  panelUI.tabbedPane.selection.index = mode match {
    case CREATION ⇒ 0
    case IO ⇒ 0
    case _ ⇒ 1
  }

  def buildProtoPanel = {
    val (implicitIP, implicitOP) = proxy.dataUI.implicitPrototypes
    new IOPrototypePanel(scene,
      this,
      proxy.dataUI.inputs,
      proxy.dataUI.outputs,
      implicitIP,
      implicitOP,
      proxy.dataUI.inputParameters.toMap)
  }
  def updateMainPanel = {

  }

  def updateProtoPanel = {
    save
    protoPanel = buildProtoPanel
    panelUI.tabbedPane.pages(1).content = protoPanel
  }

  var protoPanel = buildProtoPanel

  panelUI.tabbedPane.pages(1).content = protoPanel
  panelUI.tabbedPane.revalidate

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    contents += panelUI.tabbedPane
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)

  listenTo(panelUI.help.components.toSeq: _*)
  listenTo(panelUI.tabbedPane.selection)

  reactions += {
    case FocusGained(source: Component, _, _) ⇒
      panelUI.help.switchTo(source)
      scene.closeExtraPropertyPanel
    case ComponentFocusedEvent(source: Component) ⇒ panelUI.help.switchTo(source)
    case UpdatedPrototypeEvent(_) | SelectionChanged(panelUI.tabbedPane) ⇒
      scene.closeExtraPropertyPanel
      updateProtoPanel
  }

  def create = {
    Proxys += proxy
    ConceptMenu.taskMenu.popup.contents += ConceptMenu.addItem(nameTextField.text, proxy)
  }

  def delete = {
    val toBeRemovedCapsules: List[ICapsuleUI] = ScenesManager.moleScenes.map {
      _.manager.capsules.values.filter {
        _.dataUI.task == Some(proxy)
      }
    }.flatten.toList
    toBeRemovedCapsules match {
      case Nil ⇒
        scene.closePropertyPanel
        Proxys -= proxy
        ConceptMenu.removeItem(proxy)
        true
      case _ ⇒
        if (DialogFactory.deleteProxyConfirmation(proxy)) {
          toBeRemovedCapsules.foreach {
            c ⇒ c.scene.graphScene.removeNodeWithEdges(c.scene.manager.removeCapsuleUI(c))
          }
          delete
        } else false
    }
  }

  def save = {
    val protoPanelSave = protoPanel.save
    proxy.dataUI = panelUI.save(nameTextField.text, protoPanelSave._1, protoPanelSave._2, protoPanelSave._3)
  }
}