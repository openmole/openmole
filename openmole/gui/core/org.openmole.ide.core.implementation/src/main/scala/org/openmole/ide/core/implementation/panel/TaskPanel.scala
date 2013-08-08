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

import org.openmole.ide.core.implementation.dataproxy.{ TaskDataProxyFactory, TaskDataProxyUI, Proxies }
import org.openmole.ide.core.implementation.execution.ScenesManager
import ConceptMenu._
import org.openmole.ide.core.implementation.dialog.{ StatusBar, DialogFactory }
import org.openmole.ide.core.implementation.data._
import org.openmole.ide.core.implementation.workflow.CapsuleUI
import java.awt.{ Color, BorderLayout }
import org.openmole.misc.eventdispatcher.{ Event, EventListener, EventDispatcher }
import org.openmole.ide.core.implementation.registry.KeyRegistry
import scala.swing.event.SelectionChanged
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.Label

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
  val taskCombo = ConceptMenu.buildTaskMenu(p ⇒ updateConceptPanel(p.dataUI))

  build
  var ioSettings = ioPanel

  listenTo(panelSettings.help.components.toSeq: _*)

  def build = {
    basePanel.contents += new PluginPanel("wrap 2", "-5[left]-10[]", "-2[top][10]") {
      contents += header(scene, index)
      contents += new PluginPanel("wrap 2", "[]30[]", "") {
        contents += new Composer {
          addIcon(icon)
          addName
          addTypeMenu(taskCombo)
          addCreateLink
        }
        contents += proxyShorcut(proxy.dataUI, index)
      }
    }
    createSettings
  }

  override def created = proxyCreated

  def createSettings = {
    ioSettings = ioPanel
    if (basePanel.contents.size > 1) {
      basePanel.contents.remove(1)
      basePanel.contents.remove(1)
    }
    icon.icon = icon(proxy.dataUI).icon
    panelSettings = proxy.dataUI.buildPanelUI
    basePanel.contents += panelSettings.tabbedPane(("I/O", ioSettings))
    basePanel.contents += panelSettings.help
  }

  def updateConceptPanel(d: TaskDataUI with ImageView) = {
    savePanel
    proxy.dataUI = d
    createSettings
  }

  def savePanel = {
    val ioSave = ioSettings.save
    panelSettings.saveContent(nameTextField.text) match {
      case x: DATAUI ⇒ proxy.dataUI = save(x, ioSave._1, ioSave._2, ioSave._3)
      case _         ⇒ StatusBar().warn("The current panel cannot be saved")
    }
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