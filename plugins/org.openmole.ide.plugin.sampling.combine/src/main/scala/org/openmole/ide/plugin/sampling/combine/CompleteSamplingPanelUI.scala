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

package org.openmole.ide.plugin.sampling.combine

import scala.swing._
/import org.openmole.ide.plugin.sampling.tools.GenericSamplingPanel
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
import org.openide.util.Lookup
import org.openmole.ide.misc.widget.PluginPanel

//FIXME : Wait for the end Netbeans IDE !
class CompleteSamplingPanelUI(cud: CompleteSamplingDataUI) extends PluginPanel("wrap") with ISamplingPanelUI {
    
  val panel = new GenericSamplingPanel
  //(cud.factors,
    //                                   Lookup.getDefault.lookupAll(classOf[IDomainFactoryUI]).toList.map{_.displayName})
  //contents += panel 
  
  override def saveContent(name: String) = new CompleteSamplingDataUI(name)
                                                                      //panel.factors)
}