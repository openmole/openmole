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
import org.openide.util.lookup.InstanceContent
import org.openide.windows.WindowManager
import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.commons.MoleSceneType._

object PaletteSupport {
  val TASK_DATA_FLAVOR= new DataFlavor(classOf[TaskDataProxyUI], TASK)
  val PROTOTYPE_DATA_FLAVOR= new DataFlavor(classOf[PrototypeDataProxyUI], PROTOTYPE)
  val SAMPLING_DATA_FLAVOR= new DataFlavor(classOf[SamplingDataProxyUI], SAMPLING)
  val ENVIRONMENT_DATA_FLAVOR= new DataFlavor(classOf[EnvironmentDataProxyUI], ENVIRONMENT)

  var palette: Option[PaletteController] = None
  val ic = new InstanceContent
  // private var paletteRoot: Option[AbstractNode] = None
  
  def createPalette(mst: MoleSceneType) = {
    val paletteRoot = new AbstractNode(new CategoryBuilder(mst))
    paletteRoot.setName("Entities")
    palette = Some(PaletteFactory.createPalette(paletteRoot, new MyActions, new MyPaletteFilter, new MyDnDHandler))
    palette.get.addPropertyChangeListener( new MyAddPropertyChangeListener(palette.get)) 
    ic.add(palette);
  }
  
  def createPalette: Unit = createPalette(BUILD)
  
  def refreshPalette(mst: MoleSceneType) = {
    ic.remove(palette);
    createPalette(mst);
    ic.add(palette);
    WindowManager.getDefault.findTopComponent("MoleSceneTopComponent").asInstanceOf[MoleSceneTopComponent].updateMode( if (mst.equals(BUILD)) true else false)
  }
  
  def refreshPalette: Unit = refreshPalette(BUILD)
}
  
class MyAddPropertyChangeListener(palette: PaletteController) extends PropertyChangeListener  {
  var currentSelItem = Lookup.EMPTY
  
  override def  propertyChange(pce: PropertyChangeEvent)= {
        
    // PropertyPanel.getDefault.save
    val selItem = palette.getSelectedItem
    val selCategoryLookup = palette.getSelectedCategory.lookup(classOf[Node])
    if (selItem != null && selCategoryLookup != null && selItem != currentSelItem){
      Displays.currentType = selCategoryLookup.getName
      Displays.setAsName(selItem.lookup(classOf[Node]).getDisplayName)
      // PropertyPanel.getDefault.displayCurrentEntity(Proxys.getDataProxyUI(selCategoryLookup.getName,selItem.lookup(classOf[Node]).getName)) 
    
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