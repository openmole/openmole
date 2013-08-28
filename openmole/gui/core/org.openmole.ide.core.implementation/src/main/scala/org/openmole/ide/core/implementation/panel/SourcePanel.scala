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

import org.openmole.ide.core.implementation.dataproxy.{ Proxies, SourceDataProxyUI }
import org.openmole.ide.core.implementation.data.{ ImageView, SourceDataUI }
import scala.swing.Label
import ConceptMenu._
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.misc.tools.image.Images
import scala.swing.event.SelectionChanged

trait SourcePanel extends Base
    with Header
    with ProxyShortcut
    with IOProxy
    with ConceptCombo
    with Icon
    with IO {

  override type DATAPROXY = SourceDataProxyUI with IOFacade
  type SOURCEDATAUI = SourceDataUI with ImageView
  override type DATAUI = SOURCEDATAUI with IODATAUI

  var panelSettings = proxy.dataUI.buildPanelUI
  val icon: Label = icon(Images.SOURCE)
  val sourceCombo = ConceptMenu.buildSourceMenu(p ⇒ updateConceptPanel(p.dataUI), proxy.dataUI)

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
          addTypeMenu(sourceCombo)
          addCreateLink
        }
        contents += proxyShorcut(proxy.dataUI, index)
      }
    }
    createSettings
  }

  override def created = proxyCreated

  def createSettings: Unit = {
    panelSettings = proxy.dataUI.buildPanelUI

    val tPane = panelSettings.tabbedPane(("I/O", ioSettings))
    Tools.updateIndex(basePanel, tPane)

    if (basePanel.contents.size == 3) basePanel.contents.remove(1, 2)
    basePanel.contents += tPane
    basePanel.contents += panelSettings.help

    listenTo(tPane.selection)
    listenTo(panelSettings.help.components.toSeq: _*)

    tPane.reactions += {
      case SelectionChanged(_) ⇒ updatePanel
    }
  }

  override def updatePanel = {
    savePanel
    ioSettings = ioPanel
    createSettings
  }

  def updateConceptPanel(d: SOURCEDATAUI) = {
    savePanel
    d.inputs = ioSettings.prototypesIn
    d.outputs = ioSettings.prototypesOut
    proxy.dataUI = d
    createSettings
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
    scene.closePropertyPanel(index)
    Proxies.instance -= proxy
    -=(proxy)
    //true

  }

}