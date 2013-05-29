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

import java.awt.{ Dimension, BorderLayout }
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openide.awt.HtmlBrowser
import org.openmole.ide.core.implementation.dataproxy.{ UpdatedProxyEvent, Proxies }
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.dataproxy.{ IPrototypeDataProxyUI, ISamplingCompositionDataProxyUI, IDataProxyUI, ITaskDataProxyUI }
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow._
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabel._
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor._
import scala.collection.mutable.HashMap
import scala.swing.Component
import scala.swing.Label
import scala.swing.TabbedPane
import swing.event.{ SelectionChanged, FocusGained }
import org.openmole.ide.core.model.data.IExplorationTaskDataUI
import org.openmole.ide.core.implementation.data.TaskDataUI

class TaskPanel(proxy: ITaskDataProxyUI,
                scene: IMoleScene,
                val index: Int) extends BasePanel(Some(proxy), scene) {
  taskPanel ⇒
  iconLabel.icon = new ImageIcon(ImageIO.read(proxy.dataUI.getClass.getClassLoader.getResource(proxy.dataUI.imagePath)))

  var panelUI = proxy.dataUI.buildPanelUI
  def created = Proxies.instance.contains(proxy)

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

  def updatePanel = {
    tabbedLock = true
    save
    panelUI = proxy.dataUI.buildPanelUI
    refreshPanel
    protoPanel = buildProtoPanel
    tabbedPane.pages.insert(1, new TabbedPane.Page("Inputs / Outputs", protoPanel))
    tabbedPane.revalidate
    tabbedLock = false
  }

  def updateProtoPanel = {
    save
    protoPanel = buildProtoPanel
    tabbedPane.pages(1).content = protoPanel
    tabbedPane.revalidate
  }

  var protoPanel = buildProtoPanel

  refreshPanel

  tabbedPane.pages.insert(1, new TabbedPane.Page("Inputs / Outputs", protoPanel))

  tabbedPane.selection.index = 0
  tabbedPane.revalidate

  val newPanel = new NewConceptPanel(this)
  newPanel.addPrototype
  proxy.dataUI match {
    case d: TaskDataUI with IExplorationTaskDataUI ⇒ newPanel.addSamplingComposition
    case _                                         ⇒
  }

  mainPanel.contents += newPanel

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    contents += tabbedPane
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)

  listenTo(panelUI.help.components.toSeq: _*)
  listenTo(tabbedPane.selection)
  listenTo(panelUI)

  reactions += {
    case FocusGained(source: Component, _, _) ⇒
      panelUI.help.switchTo(source)
    //scene.closePropertyPanel(index)
    case ComponentFocusedEvent(source: Component) ⇒ panelUI.help.switchTo(source)
    case SelectionChanged(tabbedPane) ⇒
      if (!tabbedLock) updateProtoPanel
    case UpdatedProxyEvent(p: IDataProxyUI, _) ⇒
      scene.removeAll(index + 1)
      updatePanel
      p match {
        case t: ITaskDataProxyUI ⇒
        case _                   ⇒ setTab(p)
      }
  }

  def create = {
    Proxies.instance += proxy
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
        scene.closePropertyPanel(index)
        Proxies.instance -= proxy
        if (!proxy.generated) ConceptMenu.removeItem(proxy)
        true
      case _ ⇒
        if (DialogFactory.deleteProxyConfirmation(proxy)) {
          toBeRemovedCapsules.foreach {
            c ⇒ c.scene.graphScene.removeNodeWithEdges(c.scene.manager.removeCapsuleUI(c))
          }
          delete
        }
        else false
    }
  }

  def save = {
    val protoPanelSave = protoPanel.save
    proxy.dataUI = panelUI.save(nameTextField.text, protoPanelSave._1, protoPanelSave._2, protoPanelSave._3)
  }
}