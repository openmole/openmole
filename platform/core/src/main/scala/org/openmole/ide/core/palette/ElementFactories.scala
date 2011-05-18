/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.palette

import org.openide.util.Lookup
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.properties.IPrototypeFactoryUI
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.properties.IEnvironmentFactoryUI
import org.openmole.ide.core.properties.ISamplingFactoryUI
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.commons.Constants
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object ElementFactories {
  
  lazy val paletteElements = Map(Constants.TASK -> new ListBuffer[PaletteElementFactory],
                                 Constants.PROTOTYPE -> new ListBuffer[PaletteElementFactory],
                                 Constants.SAMPLING -> new ListBuffer[PaletteElementFactory],
                                 Constants.ENVIRONMENT -> new ListBuffer[PaletteElementFactory])
  
  lazy val modelElements = Map(Constants.TASK -> updateLookup(classOf[ITaskFactoryUI],Constants.TASK),
                               Constants.PROTOTYPE -> updateLookup(classOf[IPrototypeFactoryUI],Constants.PROTOTYPE),
                               Constants.SAMPLING -> updateLookup(classOf[ISamplingFactoryUI],Constants.SAMPLING),
                               Constants.ENVIRONMENT -> updateLookup(classOf[IEnvironmentFactoryUI],Constants.ENVIRONMENT))
  
  def updateLookup(factoryClass: Class[_<:IFactoryUI], entityType: String) = {
    val li = new ListBuffer[ModelElementFactory]
    Lookup.getDefault.lookupAll(factoryClass).foreach(p=>{li += new ModelElementFactory(p.displayName,entityType,p)})
    li
  }
  
  def addElement(pef: PaletteElementFactory) = paletteElements(pef.entity.entityType) += pef
  
  def getPaletteElementFactory(categoryName: String, name: String): PaletteElementFactory= {
    println ("get :: " + name)
    val paletteMap = paletteElements(categoryName).groupBy(_.displayName).filterKeys(k => k.equals(name))
    if (paletteMap.contains(name)) paletteMap(name).head
    else throw new GUIUserBadDataError("Not found entity " + name)
  }
} 