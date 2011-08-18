/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.sampling.complete

import scala.swing._
import swing.Swing._
import scala.swing.event.ButtonClicked
import swing.ListView._
import scala.swing.BoxPanel
import scala.swing.Table.ElementMode._
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import scala.swing.BorderPanel.Position._

class CompleteSamplingPanelUI(pud: CompleteSamplingDataUI) extends BoxPanel(Orientation.Vertical) with ISamplingPanelUI {
  
  Proxys.prototype.foreach(pud=>contents+= buildFactorPanel(pud))
  
  def buildFactorPanel(pud: IPrototypeDataProxyUI) = {
    val cbb = new ComboBox(List.empty)
    new BoxPanel(Orientation.Horizontal){
    contents+= new CheckBox(pud.dataUI.name) {reactions+= {case ButtonClicked(cb) =>cbb.enabled = selected}}
    contents+= cbb}
  }
  
  override def saveContent(name: String) = new CompleteSamplingDataUI(name)
}
