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

package org.openmole.ide.plugin.sampling.lhs

import scala.swing._
import swing.Swing._
import swing.ListView._
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.BorderPanel.Position._
import org.openmole.ide.plugin.sampling.tools.GenericBoundedSamplingPanel
import scala.collection.JavaConversions._

class LHSSamplingPanelUI(cud: LHSSamplingDataUI) extends PluginPanel("wrap 2","","") with ISamplingPanelUI {
  
  val sampleTextField = new TextField(cud.samples,4) 
   val panel = new GenericBoundedSamplingPanel(cud.factors, KeyRegistry.boundedDomains.map{_._2.displayName}.toList)
  
  contents += new Label("Number of samples")
  contents += sampleTextField
  contents += panel 
  
  
  override def saveContent(name: String) = new LHSSamplingDataUI(name,
                                                                 sampleTextField.text,
                                                                 panel.factors)
}
