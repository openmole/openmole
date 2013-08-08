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

trait CapsulePanel extends Base
    with Capsule
    with Header
    with ProxyShortcut {

  def components = panelSettings.components

  var panelSettings = capsule.dataUI.buildPanelUI(index)

  build

  def build = {
    basePanel.contents += panelSettings.panel
  }

  def createSettings = {
    panelSettings = capsule.dataUI.buildPanelUI(index)
    basePanel.contents += panelSettings.tabbedPane
  }

  def savePanel = {
    capsule.dataUI = panelSettings.saveContent("")
    scene.refresh
  }

}