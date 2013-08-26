/*
 * Copyright (C) 201 <mathieu.Mathieu Leclaire at openmole.org>
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

import scala.swing.{ Publisher, Label }
import scala.swing.event.SelectionChanged
import org.openmole.ide.misc.widget.PluginPanel

trait CapsulePanel extends Base
    with Capsule
    with Header
    with ProxyShortcut {

  def components = panelSettings.components

  var panelSettings = capsule.dataUI.buildPanelUI

  build

  def build = {
    basePanel.contents += header(scene, index)
    basePanel.contents += new Label("")
    basePanel.contents += proxyShorcut(capsule.dataUI, index)
    createSettings
  }

  def createSettings = {
    panelSettings = capsule.dataUI.buildPanelUI

    val tPane = panelSettings.tabbedPane
    Tools.updateIndex(basePanel, tPane)

    if (basePanel.contents.size == 3) basePanel.contents.remove(1)

    basePanel.contents.insert(1, tPane)

    tPane.listenTo(tPane.selection)

    tPane.reactions += {
      case SelectionChanged(_) ⇒ updatePanel
    }
  }

  override def updatePanel = {
    savePanel
    createSettings
  }

  def savePanel = {
    capsule.dataUI = panelSettings.saveContent("")
    scene.refresh
  }

}