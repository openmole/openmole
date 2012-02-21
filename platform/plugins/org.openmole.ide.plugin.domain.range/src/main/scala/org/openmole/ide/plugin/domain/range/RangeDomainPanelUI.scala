/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.TextField
import scala.swing.Label

class RangeDomainPanelUI(pud: RangeDomainDataUI) extends PluginPanel("fillx","[left][grow,fill]","") with IDomainPanelUI{
  val minField = new TextField(6) {text = pud.min}
  val maxField = new TextField(6) {text = pud.max}
  val stepField = new TextField(6) {text = pud.step}
  
  contents+= (new Label("Min"),"gap para")
  contents+= minField
  contents+= (new Label("Max"),"gap para")
  contents+=  maxField
  contents+= (new Label("Step"),"gap para")
  contents+= (stepField,"wrap")
  
  def saveContent(name: String) = new RangeDomainDataUI(name,
                                                        minField.text,
                                                        maxField.text,
                                                        stepField.text)
  
}
