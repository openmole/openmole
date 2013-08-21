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

import org.openmole.ide.core.implementation.sampling.DomainProxyUI
import org.openmole.ide.core.implementation.data.DomainDataUI

trait DomainPanel extends Base
    with Proxy { domainP ⇒

  type DATAPROXY = DomainProxyUI {
    type DATAUI = domainP.DATAUI
    var dataUI: DATAUI
  }

  type DATAUI = DomainDataUI
  //val self = this

  val proxy: DATAPROXY

  var panelSettings = proxy.dataUI.buildPanelUI
  build

  def build = {
    basePanel.contents += panelSettings.panel
  }

  def components = panelSettings.components

  def createSettings = {
    panelSettings = proxy.dataUI.buildPanelUI
    basePanel.contents += panelSettings.tabbedPane
  }

  def savePanel = proxy.dataUI = panelSettings.saveContent

  def deleteProxy = {}
}