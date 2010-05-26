/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ui.palette;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.openide.nodes.Node;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.workflow.implementation.Preferences;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class PrototypeChildren extends GenericChildren {

    @Override
    protected List<Node> initCollection() {
        Collection<Class> protos = Preferences.getInstance().getPrototypes();

        ArrayList childrenNodes = new ArrayList(protos.size());
        for (Class proto : protos) {
            childrenNodes.add(new PrototypeNode(ApplicationCustomize.PROTOTYPE_DATA_FLAVOR,
                                                proto));
        }
        return childrenNodes;
    }
}
