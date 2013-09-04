/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
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

import org.openmole.ide.core.implementation.dataproxy.{ Proxies, EnvironmentDataProxyUI }
import org.openmole.ide.core.implementation.data.{ ImageView, EnvironmentDataUI }
import scala.swing.Label
import ConceptMenu._
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.dialog.DialogFactory

trait EnvironmentPanel extends Base
    with Header
    with ProxyShortcut
    with Proxy
    with ConceptCombo
    with Icon {

  override type DATAPROXY = EnvironmentDataProxyUI
  type DATAUI = EnvironmentDataUI with ImageView

  var panelSettings = proxy.dataUI.buildPanelUI
  val icon: Label = fatIcon(proxy.dataUI)
  val taskCombo = ConceptMenu.buildEnvironmentMenu(p ⇒ updateConceptPanel(p.dataUI), proxy.dataUI)

  build

  listenTo(panelSettings.help.components.toSeq: _*)

  def build = {
    basePanel.contents += new PluginPanel("wrap", "-5[left]-10[]", "-2[top][10]") {
      contents += header(scene, index)
      contents += new PluginPanel("wrap") {
        contents += new Composer {
          addIcon(icon)
          addName
          addTypeMenu(taskCombo)
          addCreateLink
        }
      }
    }
    createSettings
  }

  override def created = proxyCreated

  def createSettings = {
    if (basePanel.contents.size == 3) basePanel.contents.remove(1, 2)

    icon.icon = fatIcon(proxy.dataUI).icon
    panelSettings = proxy.dataUI.buildPanelUI
    basePanel.contents += panelSettings.bestDisplay
    basePanel.contents += panelSettings.help
  }

  override def updatePanel = {
    savePanel
    createSettings
  }

  def updateConceptPanel(d: EnvironmentDataUI with ImageView) = {
    savePanel
    proxy.dataUI = d
    createSettings
  }

  def savePanel = {
    proxy.dataUI = panelSettings.saveContent(nameTextField.text)
    ConceptMenu.refreshItem(proxy)
  }

  def deleteProxy = {
    val capsulesWithEnv = ScenesManager.moleScenes.flatMap {
      _.dataUI.capsules.values.filter {
        _.dataUI.environment == Some(proxy)
      }
    }.toList
    capsulesWithEnv match {
      case Nil ⇒
        scene.closePropertyPanel(index)
        Proxies.instance -= proxy
        -=(proxy)
      case _ ⇒
        if (DialogFactory.deleteProxyConfirmation(proxy)) {
          scene.closePropertyPanel(index)
          capsulesWithEnv.foreach {
            _.environment_=(None)
          }
        }
    }
  }

}