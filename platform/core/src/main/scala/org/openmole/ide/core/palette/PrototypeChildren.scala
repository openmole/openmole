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

import java.util.List
import java.util.ArrayList
import org.openide.nodes.Node
import org.openmole.ide.core.commons.ApplicationCustomize
import org.openmole.ide.core.workflow.model.IEntityUI;
import org.openmole.ide.core.workflow.implementation.PrototypesUI

class PrototypeChildren extends GenericChildren {
  override def initCollection: java.util.List[Node] = {
    val childrenNodes = new ArrayList[Node](PrototypesUI.getAll.size)
    PrototypesUI.getAll.foreach(p=>{childrenNodes.add(new PrototypeNode(ApplicationCustomize.PROTOTYPE_DATA_FLAVOR,p))})
    childrenNodes
  }


}
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import org.openide.nodes.Node;
//import org.openmole.commons.exception.UserBadDataError;
//import org.openmole.ide.core.commons.ApplicationCustomize;
//import org.openmole.ide.core.exception.MoleExceptionManagement;
//import org.openmole.ide.core.workflow.implementation.IEntityUI;
//import org.openmole.ide.core.workflow.implementation.PrototypesUI;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
// */
//public class PrototypeChildren extends GenericChildren {
//
//    @Override
//    protected List<Node> initCollection() {
//        Collection<IEntityUI> prototypes = PrototypesUI.getInstance().getAll();
//
//        ArrayList childrenNodes = new ArrayList(prototypes.size());
//        for (IEntityUI proto : prototypes) {
//            try {
//                childrenNodes.add(new PrototypeNode(ApplicationCustomize.PROTOTYPE_DATA_FLAVOR, proto));
//            } catch (UserBadDataError ex) {
//                MoleExceptionManagement.showException(ex);
//            }
//        }
//        return childrenNodes;
//    }
//}