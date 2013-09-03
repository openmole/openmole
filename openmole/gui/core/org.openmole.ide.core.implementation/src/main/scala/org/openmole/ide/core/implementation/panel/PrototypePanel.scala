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

import org.openmole.ide.core.implementation.dataproxy.{ TaskDataProxyUI, Proxies, PrototypeDataProxyUI }
import org.openmole.ide.core.implementation.panel.ConceptMenu._
import org.openmole.ide.core.implementation.data.{ PrototypeDataUI, ImageView }
import scala.swing.Label
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.workflow.CapsuleUI
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI

trait PrototypePanel extends Base
    with Header
    with ProxyShortcut
    with Proxy
    with ConceptCombo
    with Icon {

  override type DATAPROXY = PrototypeDataProxyUI
  type DATAUI = GenericPrototypeDataUI[_] with ImageView

  var panelSettings = proxy.dataUI.buildPanelUI
  val icon: Label = fatIcon(proxy.dataUI)
  val protoCombo = ConceptMenu.buildPrototypeMenu(p ⇒ updateConceptPanel(p.dataUI), proxy.dataUI)

  private def updateConceptPanel(d: PrototypeDataUI[_] with ImageView) = {
    savePanel
    proxy.dataUI = d
    createSettings
    scene.updatePanels
  }

  build

  listenTo(panelSettings.help.components.toSeq: _*)

  def build = {
    basePanel.contents += new PluginPanel("wrap", "-5[left]-10[]", "-2[top][10]") {
      contents += header(scene, index)
      contents += new PluginPanel("wrap") {
        contents += new Composer {
          addIcon(icon)
          addName
          addTypeMenu(protoCombo)
          addCreateLink
        }
      }
    }
    createSettings
  }

  def createSettings = {
    if (basePanel.contents.size > 1) {
      basePanel.contents.remove(1)
      basePanel.contents.remove(1)
    }
    icon.icon = fatIcon(proxy.dataUI).icon
    panelSettings = proxy.dataUI.buildPanelUI
    basePanel.contents += panelSettings.bestDisplay
    basePanel.contents += panelSettings.help
  }

  override def created = proxyCreated

  override def updatePanel = {
    savePanel
  }

  def savePanel = {
    proxy.dataUI = panelSettings.saveContent(nameTextField.text)
    ConceptMenu.refreshItem(proxy)
  }

  def deleteProxy = {

    //remove in Tasks
    /* val capsulesWithProtos: List[CapsuleUI] = ScenesManager.moleScenes.flatMap {
      _.dataUI.capsules.values.flatMap { c ⇒
        c.dataUI.task match {
          case Some(x: TaskDataProxyUI) ⇒ if (x.dataUI.filterPrototypeOccurencies(proxy).isEmpty) None else Some(c)
          case _                        ⇒ None
        }
      }
    }.toList */

    scene.closePropertyPanel(index)
    Proxies.instance -= proxy
    -=(proxy)

    if (DialogFactory.deleteProxyConfirmation(proxy)) {
      Proxies.instance.hooks.foreach { h ⇒
        h.dataUI = h.dataUI.doClone(proxy)
      }

      Proxies.instance.sources.foreach { s ⇒
        s.dataUI = s.dataUI.doClone(proxy)
      }

      Proxies.instance.tasks.foreach { s ⇒
        s.dataUI = s.dataUI.doClone(proxy)
      }

      List(ScenesManager.currentScene).flatten.foreach {
        _.dataUI.connectors.values.toList.foreach {
          dc ⇒ dc.filteredPrototypes = dc.filteredPrototypes.filterNot { _ == proxy }
        }
      }
      ScenesManager.invalidateMoles

    }
  }

}