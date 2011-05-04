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
import org.openmole.ide.core.workflow.action.PaletteItemAction

object PaletteSupport {
  
  def createPalette = {
    val paletteRoot = new AbstractNode(new CategoryBuilder)
    paletteRoot.setName("Palette Root")
    //  Preferences.clearProperties
    //  Preferences.clearModels
    val palette = PaletteFactory.createPalette(paletteRoot, new MyActions, new MyPaletteFilter, new MyDnDHandler)
    palette.addPropertyChangeListener( new MyPropertyChangeListener(palette))
    palette
  }
}
  
class MyPropertyChangeListener(palette: PaletteController) extends PropertyChangeListener  {
  override def  propertyChange(pce: PropertyChangeEvent)= {
    val selItem = palette.getSelectedItem
    val selCategoryLookup = palette.getSelectedCategory.lookup(classOf[Node])
    if (selItem != null && selCategoryLookup != null){
      println("OB :: " + PaletteElementFactories.getFactoryUI(selCategoryLookup.getName,selItem.lookup(classOf[Node]).getName).coreObject(selItem.lookup(classOf[Node]).getName).toString)   
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

//
//import java.awt.Image;
//import java.awt.datatransfer.DataFlavor;
//import java.awt.datatransfer.UnsupportedFlavorException;
//import java.beans.BeanInfo;
//import java.io.IOException;
//import javax.swing.Action;
//import org.netbeans.spi.palette.DragAndDropHandler;
//import org.netbeans.spi.palette.PaletteActions;
//import org.netbeans.spi.palette.PaletteController;
//import org.netbeans.spi.palette.PaletteFactory;
//import org.netbeans.spi.palette.PaletteFilter;
//import org.openide.nodes.AbstractNode;
//import org.openide.nodes.Node;
//import org.openide.util.Lookup;
//import org.openide.util.datatransfer.ExTransferable;
//import org.openmole.ide.core.workflow.implementation.Preferences;
//
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
// */
//public class PaletteSupport {
//    private static PaletteController controller;
//    
//    public static PaletteController createPalette(){
//        AbstractNode paletteRoot = new AbstractNode(new CategoryBuilder());
//        paletteRoot.setName("Palette Root");
//        Preferences.getInstance().clearProperties();
//        Preferences.getInstance().clearModels();
//        controller = PaletteFactory.createPalette(paletteRoot, new MyActions(), new MyPaletteFilter(), new MyDnDHandler());
//        return controller;
//    }
//
//    private static class MyActions extends PaletteActions {
//
//        @Override
//        public Action[] getImportActions() {
//            return null;
//        }
//
//        @Override
//        public Action[] getCustomPaletteActions() {
//            return null;
//        }
//
//        @Override
//        public Action[] getCustomCategoryActions(Lookup lookup) {
//            return null;
//        }
//
//        @Override
//        public Action[] getCustomItemActions(Lookup lookup) {
//            return null;
//        }
//
//        @Override
//        public Action getPreferredAction(Lookup lookup) {
//            return null;
//        }
//    }
//
//    public static class MyPaletteFilter extends PaletteFilter {
//
//        public static void refresh(){
//            controller.refresh();
//        }
//        
//        @Override
//        public boolean isValidCategory(Lookup lkp) {
//            return true;
//        }
//
//        @Override
//        public boolean isValidItem(Lookup lkp) {
//            return true;
//        }
//        
//    }
//    
//    private static class MyDnDHandler extends DragAndDropHandler {
//
//        @Override
//        public void customize(ExTransferable exTransferable, Lookup lookup) {
//            Node node = lookup.lookup(Node.class);
//            final Image image = (Image) node.getIcon(BeanInfo.ICON_COLOR_16x16);
//            exTransferable.put(new ExTransferable.Single(DataFlavor.imageFlavor) {
//
//                @Override
//                protected Object getData() throws IOException, UnsupportedFlavorException {
//                    return image;
//                }
//            });
//        }
//    }
//}