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

import org.openmole.ide.core.implementation.dataproxy.{ Proxies, SamplingCompositionDataProxyUI }
import org.openmole.ide.core.implementation.sampling.SamplingCompositionDataUI
import org.openmole.ide.core.implementation.data.ImageView
import scala.swing.{ Publisher, Label }
import org.openmole.ide.misc.tools.image.Images
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.dialog.DialogFactory

trait SamplingCompositionPanel extends Base
    with Publisher
    with Header
    with ProxyShortcut
    with Proxy
    with Icon {

  override type DATAPROXY = SamplingCompositionDataProxyUI
  type DATAUI = SamplingCompositionDataUI with ImageView

  var panelSettings = proxy.dataUI.buildPanelUI
  val icon: Label = icon(Images.SAMPLING_COMPOSITION_FAT)

  build

  listenTo(panelSettings.help.components.toSeq: _*)

  def build = {
    basePanel.contents += new PluginPanel("wrap", "-5[left]-10[]", "-2[top][10]") {
      contents += header(scene, index)
      contents += new PluginPanel("wrap") {
        contents += new Composer {
          addIcon(icon)
          addName
          addCreateLink
        }
        contents += proxyShorcut(proxy.dataUI, index)
      }
    }
    createSettings
  }

  override def created = proxyCreated

  def createSettings = {
    if (basePanel.contents.size == 3) basePanel.contents.remove(1, 2)

    panelSettings = proxy.dataUI.buildPanelUI
    basePanel.contents += panelSettings.bestDisplay
    basePanel.contents += panelSettings.help
  }

  override def updatePanel = {
    savePanel
    createSettings
  }

  def updateConceptPanel(d: SamplingCompositionDataUI with ImageView) = {
    savePanel
    proxy.dataUI = d
    createSettings
  }

  def savePanel = {
    proxy.dataUI = panelSettings.saveContent(nameTextField.text)
    ConceptMenu.refreshItem(proxy)
  }

  def deleteProxy = {
    val toBeRemovedSamplings = ScenesManager.explorationCapsules.filter { case (c, d) ⇒ d.sampling == Some(proxy) }
    toBeRemovedSamplings match {
      case Nil ⇒
        scene.closePropertyPanel(index)
        Proxies.instance -= proxy
        ConceptMenu.-=(proxy)
      // true
      case _ ⇒
        if (DialogFactory.deleteProxyConfirmation(proxy)) {
          toBeRemovedSamplings.foreach { case (c, d) ⇒ c.scene.graphScene.removeNodeWithEdges(c.scene.dataUI.removeCapsuleUI(c)) }
          deleteProxy
        }
      // else false
    }
  }

}