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

import java.awt.Image
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.Action
import org.netbeans.spi.palette.DragAndDropHandler
import org.netbeans.spi.palette.PaletteActions
import org.netbeans.spi.palette.PaletteController
import org.netbeans.spi.palette.PaletteFactory
import org.netbeans.spi.palette.PaletteFilter
import org.openide.nodes.AbstractNode
import org.openide.util.Lookup
import org.openide.nodes.Node
import java.beans.BeanInfo
import java.awt.datatransfer.DataFlavor
import org.openide.util.datatransfer.ExTransferable
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.implementation.display.Displays
import org.openide.windows.WindowManager
import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.commons.MoleSceneType._

object PaletteSupport {
  val TASK_DATA_FLAVOR= new DataFlavor(classOf[TaskDataProxyUI], TASK)
  val PROTOTYPE_DATA_FLAVOR= new DataFlavor(classOf[PrototypeDataProxyUI], PROTOTYPE)
  val SAMPLING_DATA_FLAVOR= new DataFlavor(classOf[SamplingDataProxyUI], SAMPLING)
  val ENVIRONMENT_DATA_FLAVOR= new DataFlavor(classOf[EnvironmentDataProxyUI], ENVIRONMENT)
  val DOMAIN_DATA_FLAVOR= new DataFlavor(classOf[DomainDataProxyUI], DOMAIN)

  var currentType = BUILD
  
  def createPalette(mst: MoleSceneType): PaletteController = {
    currentType = mst
    val paletteRoot = new AbstractNode(new CategoryBuilder(mst))
    paletteRoot.setName("Entities")
    var palette = PaletteFactory.createPalette(paletteRoot, new MyActions, new MyPaletteFilter, new MyDnDHandler)
    palette.addPropertyChangeListener( new MyAddPropertyChangeListener) 
    palette
  }
  
  def createPalette: PaletteController = createPalette(currentType)
  
  def refreshPalette(mst: MoleSceneType) = {
    currentType = mst
    WindowManager.getDefault.findTopComponent("MoleSceneTopComponent").asInstanceOf[MoleSceneTopComponent].refresh(if(mst.equals(BUILD)) true else false)
  }
    
  def refreshPalette: Unit = refreshPalette(currentType)
}
  
class MyAddPropertyChangeListener extends PropertyChangeListener  {
  var currentSelItem = Lookup.EMPTY
  
  override def  propertyChange(pce: PropertyChangeEvent)= {
    val mstc = WindowManager.getDefault.findTopComponent("MoleSceneTopComponent").asInstanceOf[MoleSceneTopComponent]
    val selItem = mstc.getPalette.getSelectedItem
    val selCategoryLookup = mstc.getPalette.getSelectedCategory.lookup(classOf[Node])
    if (selItem != null && selCategoryLookup != null && selItem != currentSelItem){
      Displays.currentType = selCategoryLookup.getName
    //  Displays.setAsName(selItem.lookup(classOf[Node]).getDisplayName)
    Displays.setCurrentProxyID( selItem.lookup(classOf[Node]).getValue("proxy").asInstanceOf[Int])
      Displays.propertyPanel.displayCurrentEntity 
      currentSelItem = selItem
    }
  }
}                                
                                      
                                      
class MyActions extends PaletteActions{
  override def getImportActions = null
  override def getCustomPaletteActions = null
  override def getCustomCategoryActions(lkp: Lookup) = null
  override def getCustomItemActions(lkp: Lookup) = null
  override def getPreferredAction(lkp: Lookup) = null
}
  
class MyPaletteFilter extends PaletteFilter {
  override def isValidCategory(lkp: Lookup)= true
  override def isValidItem(lkp: Lookup)= true
}
  
class MyDnDHandler extends DragAndDropHandler {
  override def customize(exTransferable: ExTransferable, lookup: Lookup)= {
    val node = lookup.lookup(classOf[Node])
    val image = node.getIcon(BeanInfo.ICON_COLOR_16x16).asInstanceOf[Image]
    exTransferable.put(new ExTransferable.Single(DataFlavor.imageFlavor) {
        override def getData: Image= return image
      })
  }
}