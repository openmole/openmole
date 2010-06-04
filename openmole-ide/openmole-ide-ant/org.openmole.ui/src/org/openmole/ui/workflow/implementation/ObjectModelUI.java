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
package org.openmole.ui.workflow.implementation;

import java.awt.Image;
import org.openmole.ui.workflow.model.IObjectModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public abstract class ObjectModelUI implements IObjectModelUI {

    private String name;
    private String modelGroup;
    private Image img = null;

    @Override
    public void objectChanged(Object obj) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getModelGroup() {
        return modelGroup;
    }
/*
    @Override
    public Image getIcon() {
        if (img == null) {
            loadIcon();
        }
        return img;
    }

    @Override
    public void setIcon(Image img) {
        this.img = img;
    }*/
}
