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

import java.awt.datatransfer.DataFlavor;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.lookup.Lookups;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.ui.exception.MoleExceptionManagement;
import org.openmole.ui.palette.Category.CategoryName;
import org.openmole.ui.workflow.implementation.Preferences;
import org.openmole.ui.workflow.implementation.PropertyManager;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class GenericNode extends AbstractNode {

    protected  DataFlavor dataFlavor;

    public GenericNode(DataFlavor key,
                       CategoryName type,
                       Class coreClass) {
        super(Children.LEAF, Lookups.fixed(new Object[]{key}));
        try {
            this.dataFlavor = key;
            setIconBaseWithExtension(Preferences.getInstance().getProperties(type, coreClass).getProperty(PropertyManager.THUMB_IMG));
            setName(Preferences.getInstance().getProperties(type, coreClass).getProperty(PropertyManager.NAME));
        } catch (UserBadDataError ex) {
            MoleExceptionManagement.showException(ex);
        }
    }
}
