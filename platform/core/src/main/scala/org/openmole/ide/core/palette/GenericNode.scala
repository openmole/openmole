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

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import org.openide.nodes.AbstractNode
import org.openide.util.datatransfer.ExTransferable
import org.openide.util.lookup.Lookups
import org.openide.nodes.Children
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
//class GenericNode(dataFlavor: DataFlavor,factory: IFactoryUI,displayName: String) extends AbstractNode(Children.LEAF, Lookups.fixed(Array[Object](dataFlavor))){
//  setIconBaseWithExtension(factory.imagePath)
//  setName(displayName)
//}

class GenericNode(dataFlavor: DataFlavor,val elementFactory: PaletteElementFactory) extends AbstractNode(Children.LEAF, Lookups.fixed(Array[Object](dataFlavor))) {
  setIconBaseWithExtension(elementFactory.factoryUI.imagePath)
  setName(elementFactory.displayName)
  
  override def drag: Transferable = {
    println("DRAG")
    val retValue = ExTransferable.create(super.drag)
    retValue.put( new ExTransferable.Single(dataFlavor) {override def getData: Object = return elementFactory})
    retValue
  }

}

//package org.openmole.ide.core.palette;
//
//import java.awt.datatransfer.DataFlavor;
//import org.openide.nodes.AbstractNode;
//import org.openide.nodes.Children;
//import org.openide.util.lookup.Lookups;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
// */
//public class GenericNode extends AbstractNode {
//
//    protected  DataFlavor dataFlavor;
//
//         public GenericNode(DataFlavor key,
//                       String iconPath,
//                       String iconName) {
//        super(Children.LEAF, Lookups.fixed(new Object[]{key}));
//            this.dataFlavor = key;
//            setIconBaseWithExtension(iconPath);
//            setName(iconName);
//
//             System.out.println("*********** "+key + ", " + iconPath +", "+iconName);
//    }
//}