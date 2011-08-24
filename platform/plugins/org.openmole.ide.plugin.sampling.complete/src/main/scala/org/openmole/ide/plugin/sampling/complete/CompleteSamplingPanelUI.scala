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
import org.openmole.ide.core.model.dataproxy.IDomainDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.misc.widget.MigPanel
import scala.None
import scala.collection.mutable.ListBuffer
import scala.swing.BorderPanel.Position._

class CompleteSamplingPanelUI(pud: CompleteSamplingDataUI) extends MigPanel("wrap 2","","") with ISamplingPanelUI {
  
  Proxys.prototype.foreach(pud=>buildRow(pud))
  
  override def saveContent(name: String) = new CompleteSamplingDataUI(name,contents.flatMap{
        case fp: FactorPanel=> if (fp.selected) List(fp.factor) else None
        case _=> None})
  
  def buildRow(pud: IPrototypeDataProxyUI) = {
    val cbb = new ComboBox(Proxys.domain.toList) {enabled = false}
    contents+= (new CheckBox(pud.dataUI.name) {reactions+= {case ButtonClicked(cb) =>cbb.enabled = selected}},"gap para")
    contents+= cbb
    new FactorPanel(pud,cbb)
  }
  
  class FactorPanel(pud: IPrototypeDataProxyUI,cbb: ComboBox[IDomainDataProxyUI]){
    def selected = cbb.enabled
    def factor= new Factor(pud.dataUI.coreObject.asInstanceOf[IPrototype[Any]],cbb.selection.item.dataUI.coreObject)
  }
}
