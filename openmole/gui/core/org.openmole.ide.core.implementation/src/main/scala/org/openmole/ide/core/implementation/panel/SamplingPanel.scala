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

import org.openmole.ide.core.implementation.data.{ SamplingDataUI }
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.sampling.SamplingPanelUI

trait SamplingPanel extends Base
    with SWidget
    with Header {

  type DATAUI = SamplingDataUI

  val samplingPanelUI = new SamplingPanelUI(widget)
  build

  def build = {
    basePanel.contents += new PluginPanel("wrap", "-5[left]-10[]", "-2[top][10]") {
      contents += header(scene, index)
    }
    createSettings
  }

  def components = samplingPanelUI.bestDisplay

  def createSettings = {
    savePanel
    widget.update
    basePanel.contents += samplingPanelUI.bestDisplay
  }

  def savePanel = widget.proxy.dataUI = samplingPanelUI.saveContent

  def deleteProxy = {}

  override def toDoOnClose = {
    widget.update
  }
}