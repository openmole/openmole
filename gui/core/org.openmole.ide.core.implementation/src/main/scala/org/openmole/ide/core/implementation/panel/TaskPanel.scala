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
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.execution.ScenesManager
import javax.swing.plaf.basic.BasicTabbedPaneUI
import org.openide.awt.HtmlBrowser
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.data.IExplorationTaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.misc.widget.multirow.MultiWidget.CLOSE_IF_EMPTY
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow._
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabel._
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor._
import scala.collection.mutable.HashMap
import scala.swing.Action
import scala.swing.MyComboBox
import scala.swing.Component
import scala.swing.Label
import scala.swing.Separator
import scala.collection.JavaConversions._
import org.openmole.ide.misc.tools.image.Images._
import scala.swing.TabbedPane
import scala.swing.event.FocusGained

class TaskPanel(proxy: ITaskDataProxyUI,
                scene: IMoleScene,
                mode: Value = CREATION) extends BasePanel(Some(proxy), scene, mode) {
  taskPanel ⇒
  iconLabel.icon = new ImageIcon(ImageIO.read(proxy.dataUI.getClass.getClassLoader.getResource(proxy.dataUI.imagePath)))
  val panelUI = proxy.dataUI.buildPanelUI

  val protoPanel = Proxys.prototypes.size match {
    case 0 ⇒
      StatusBar().inform("No Prototype has been created yet")
      panelUI.tabbedPane.pages.insert(1, new TabbedPane.Page("Inputs / Outputs", new Label("First define Prototypes !")))
      None
    case _ ⇒
      val (implicitIP, implicitOP) = proxy.dataUI.implicitPrototypes
      val iop = Some(new IOPrototypePanel(scene,
        proxy.dataUI.inputs,
        proxy.dataUI.outputs,
        implicitIP,
        implicitOP,
        proxy.dataUI.inputParameters.toMap))
      panelUI.tabbedPane.pages.insert(0, new TabbedPane.Page("Inputs / Outputs", iop.get))
      iop
  }

  panelUI.tabbedPane.selection.index = mode match {
    case CREATION ⇒ 0
    case IO ⇒ 0
    case _ ⇒ 1
  }

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    contents += panelUI.tabbedPane
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)

  listenTo(panelUI.help.components.toSeq: _*)
  reactions += {
    case FocusGained(source: Component, _, _) ⇒ panelUI.help.switchTo(source)
    case ComponentFocusedEvent(source: Component) ⇒ panelUI.help.switchTo(source)
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
    val protoPanelSave = IOPrototypePanel.save(protoPanel)
    proxy.dataUI = panelUI.save(nameTextField.text, protoPanelSave._1, protoPanelSave._2, protoPanelSave._3)

    ScenesManager.capsules(proxy).foreach {
      c ⇒
        proxy.dataUI match {
          case x: IExplorationTaskDataUI ⇒ c.setSampling(x.sampling)
          case _ ⇒
        }
    }
  }
}