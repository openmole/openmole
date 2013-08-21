/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

import org.openmole.ide.core.implementation.dataproxy.{ TaskDataProxyUI, Proxies }
import org.openmole.ide.core.implementation.execution.ScenesManager
import ConceptMenu._
import org.openmole.ide.core.implementation.dialog.{ StatusBar, DialogFactory }
import org.openmole.ide.core.implementation.data._
import org.openmole.ide.core.implementation.workflow.CapsuleUI
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.Label
import scala.swing.event.SelectionChanged

trait TaskPanel extends Base
    with Header
    with ProxyShortcut
    with IOProxy
    with ConceptCombo
    with Icon
    with IO {

  override type DATAPROXY = TaskDataProxyUI with IOFacade
  type TASKDATAUI = TaskDataUI with ImageView
  override type DATAUI = TASKDATAUI with IODATAUI

  var panelSettings = proxy.dataUI.buildPanelUI

  val icon: Label = icon(proxy.dataUI)
  val taskCombo = ConceptMenu.buildTaskMenu(p ⇒ updateConceptPanel(p.dataUI), proxy.dataUI)

  var ioSettings = ioPanel
  build

  listenTo(panelSettings.help.components.toSeq: _*)

  def build = {
    basePanel.contents += new PluginPanel("wrap 2", "-5[left]-10[]", "-2[top][10]") {
      contents += header(scene, index)
      contents += new PluginPanel("wrap 2", "[]10[]", "") {
        contents += new Composer {
          addIcon(icon)
          addName
          addTypeMenu(taskCombo)
          addCreateLink
        }
        contents += proxyShorcut(proxy.dataUI, index)
      }
    }
    createSettings()
  }

  override def created = proxyCreated

  override def update = {
    savePanel
    ioSettings = ioPanel
    createSettings()
  }

  def createSettings(curIndex: Int): Unit = {
    icon.icon = icon(proxy.dataUI).icon
    // val curIndex = tabIndex(basePanel)

    panelSettings = proxy.dataUI.buildPanelUI
    val tPane = panelSettings.tabbedPane(("I/O", ioSettings))
    tPane.selection.index = curIndex

    if (basePanel.contents.size == 3) basePanel.contents.remove(1, 2)

    basePanel.contents += tPane
    basePanel.contents += panelSettings.help

    listenTo(panelSettings.help.components.toSeq: _*)
    tPane.listenTo(tPane.selection)

    tPane.reactions += {
      case SelectionChanged(_) ⇒ update
    }
  }

  def updateConceptPanel(d: TaskDataUI with ImageView) = {
    savePanel
    d.inputs = ioSettings.prototypesIn
    d.outputs = ioSettings.prototypesOut
    proxy.dataUI = d
    createSettings(0)
  }

  def savePanel = {
    val ioSave = ioSettings.save
    panelSettings.saveContent(nameTextField.text) match {
      case x: DATAUI ⇒ proxy.dataUI = save(x, ioSave._1, ioSave._2, ioSave._3)
      case _         ⇒ StatusBar().warn("The current panel cannot be saved")
    }
    ConceptMenu.refreshItem(proxy)
  }

  def deleteProxy = {
    val toBeRemovedCapsules: List[CapsuleUI] = ScenesManager.moleScenes.map {
      _.dataUI.capsules.values.filter {
        c ⇒
          c.dataUI.task == Some(proxy)
      }
    }.flatten.toList
    toBeRemovedCapsules match {
      case Nil ⇒
        scene.closePropertyPanel(index)
        Proxies.instance -= proxy
        if (!proxy.generated) -=(proxy)
      // true
      case _ ⇒
        if (DialogFactory.deleteProxyConfirmation(proxy)) {
          toBeRemovedCapsules.foreach {
            c ⇒ c.scene.graphScene.removeNodeWithEdges(c.scene.dataUI.removeCapsuleUI(c))
          }
          ScenesManager.invalidateSceneCaches
          ScenesManager.refreshScenes
        }
      //else false
    }
  }

}