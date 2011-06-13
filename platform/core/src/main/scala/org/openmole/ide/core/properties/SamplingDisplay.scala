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

package org.openmole.ide.core.properties

import org.openide.util.Lookup
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.palette.SamplingDataProxyFactory
import org.openmole.ide.core.exception.GUIUserBadDataError

object SamplingDisplay extends IDisplay{
  private var count= 0
  private var modelSamplings = new HashSet[SamplingDataProxyFactory]
  var name = "sampling0"
  var currentPanel: Option[ISamplingPanelUI] = None
  println("INIT SAMPLING :: ")
  Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI]).foreach(f=>{modelSamplings += new SamplingDataProxyFactory(f)})
  
  override def implementationClasses = modelSamplings
  
  override def dataProxyUI(n: String) = ElementFactories.getSamplingDataProxyUI(n)
  
  override def increment = {
    count += 1
    name = "sampling" + count
  }
  
  override def  buildPanelUI(n:String) = {
    currentPanel = Some(dataProxyUI(n).dataUI.buildPanelUI)
    currentPanel.get
  }
  
  override def saveContent(oldName: String) = dataProxyUI(oldName).dataUI = currentPanel.getOrElse(throw new GUIUserBadDataError("No panel to print for entity " + oldName)).saveContent(name)
  
}