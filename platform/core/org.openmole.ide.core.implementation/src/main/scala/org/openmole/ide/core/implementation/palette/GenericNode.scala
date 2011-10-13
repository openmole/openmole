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

package org.openmole.ide.core.implementation.palette

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import org.netbeans.spi.palette.PaletteController
import org.openide.nodes.AbstractNode
import org.openide.util.datatransfer.ExTransferable
import org.openide.util.lookup.Lookups
import org.openide.nodes.Children
import org.openmole.ide.core.model.data.IDataUI
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.misc.image.ImageTool

class GenericNode(dataFlavor: DataFlavor,val dataProxy: IDataProxyUI) extends AbstractNode(Children.LEAF, Lookups.fixed(Array[Object](dataFlavor))) {
  setIconBaseWithExtension(dataProxy.dataUI.imagePath)
  setName(dataProxy.dataUI.name)
  setValue(PaletteController.ATTR_IS_READONLY, false)
  
  override def drag: Transferable = {
    val retValue = ExTransferable.create(super.drag)
    retValue.put( new ExTransferable.Single(dataFlavor) {override def getData: Object = return dataProxy})
    retValue
  }
  
  override def canDestroy = true
  
  override def destroy = {
    println("destroy")
  }

}