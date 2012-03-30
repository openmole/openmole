/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.domain.range

import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.Label
import scala.swing.TextField

class BoundedRangeDomainPanelUI (bud: BoundedRangeDomainDataUI) extends PluginPanel("fillx","[left][grow,fill]","") with IDomainPanelUI{
  val minField = new TextField(6) {text = bud.min}
  val maxField = new TextField(6) {text = bud.max}
    
  contents+= (new Label("Min"),"gap para")
  contents+= minField
  contents+= (new Label("Max"),"gap para")
  contents+=  maxField
  
  def saveContent(name: String) = new BoundedRangeDomainDataUI(name,
                                                               minField.text,
                                                               maxField.text)
  
}