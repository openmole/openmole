/*
 *  Copyright (C) 2011 Mathieu Leclaire <mathieu.leclaire@openmole.org>
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

package org.openmole.ui.ide.palette;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.openide.nodes.Node;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.ui.ide.commons.ApplicationCustomize;
import org.openmole.ui.ide.exception.MoleExceptionManagement;
import org.openmole.ui.ide.workflow.implementation.IEntityUI;
import org.openmole.ui.ide.workflow.implementation.SamplingsUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class SamplingChildren extends GenericChildren{

    @Override
    protected List<Node> initCollection() {

        Collection<IEntityUI> samplings = SamplingsUI.getInstance().getAll();
        ArrayList childrenNodes = new ArrayList(samplings.size());
        for (IEntityUI sampling : samplings) {
            try {
                childrenNodes.add(new SamplingNode(ApplicationCustomize.SAMPLING_DATA_FLAVOR,
                        sampling));
            } catch (UserBadDataError ex) {
                System.out.println("-- EXECCP ");
                MoleExceptionManagement.showException(ex);
            }
        }
        return childrenNodes;

    }
}
