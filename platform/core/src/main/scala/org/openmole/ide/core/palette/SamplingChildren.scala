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

import java.util.ArrayList
import org.openide.nodes.Node
import org.openmole.ide.core.properties.ISamplingFactoryUI
import org.openmole.ide.core.workflow.implementation.SamplingsUI
import org.openide.util.Lookup
import org.openmole.ide.core.commons.ApplicationCustomize
import scala.collection.JavaConversions._

class SamplingChildren extends GenericChildren{

  override def initCollection: java.util.List[Node] = {
    val lookup=  Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI])
    val childrenNodes = new ArrayList[Node](lookup.size)
    lookup.foreach(s=>{childrenNodes.add(new SamplingNode(ApplicationCustomize.SAMPLING_DATA_FLAVOR,s))})    
   // SamplingsUI.getAll.foreach(s=>{childrenNodes.add(new SamplingNode(ApplicationCustomize.SAMPLING_DATA_FLAVOR,s))})
    childrenNodes
  }
}
//package org.openmole.ide.core.palette;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import org.openide.nodes.Node;
//import org.openmole.commons.exception.UserBadDataError;
//import org.openmole.ide.core.commons.ApplicationCustomize;
//import org.openmole.ide.core.exception.MoleExceptionManagement;
//import org.openmole.ide.core.workflow.implementation.IEntityUI;
//import org.openmole.ide.core.workflow.implementation.SamplingsUI;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
// */
//public class SamplingChildren extends GenericChildren{
//
//    @Override
//    protected List<Node> initCollection() {
//
//        Collection<IEntityUI> samplings = SamplingsUI.getInstance().getAll();
//        ArrayList childrenNodes = new ArrayList(samplings.size());
//        for (IEntityUI sampling : samplings) {
//            try {
//                childrenNodes.add(new SamplingNode(ApplicationCustomize.SAMPLING_DATA_FLAVOR,
//                        sampling));
//            } catch (UserBadDataError ex) {
//                System.out.println("-- EXECCP ");
//                MoleExceptionManagement.showException(ex);
//            }
//        }
//        return childrenNodes;
//
//    }
//}