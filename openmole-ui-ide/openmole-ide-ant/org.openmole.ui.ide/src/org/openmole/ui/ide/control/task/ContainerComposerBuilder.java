/*
 *  Copyright (C) 2010 mathieu
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
package org.openmole.ui.ide.control.task;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JSplitPane;

/**
 *
 * @author mathieu
 */
public class ContainerComposerBuilder {

    private int splitOrientation = JSplitPane.VERTICAL_SPLIT;
    private String orientation = BorderLayout.WEST;
    private Set<OrientedComponent> orientedComponents = new HashSet<OrientedComponent>();


    public ContainerComposer build() {
        return new ContainerComposer(orientedComponents);
    }

    public ContainerComposerBuilder addComponent(Component c) {
        orientedComponents.add(new OrientedComponent(splitOrientation,
                orientation,
                c));
        return this;
    }

    public ContainerComposerBuilder setOrientation(String orientation) {
        this.orientation = orientation;
        return this;
    }

    public ContainerComposerBuilder setSplitOrientation(int splitOrientation) {
        this.splitOrientation = splitOrientation;
        return this;
    }

    public class OrientedComponent {

        private int splitOrientation;
        private String orientation;
        private Component component;

        public OrientedComponent(int splitOrientation,
                String orientation,
                Component component) {
            this.orientation = orientation;
            this.component = component;
            this.splitOrientation = splitOrientation;
        }

        public Component getComponent() {
            return component;
        }

        public String getOrientation() {
            return orientation;
        }

        public int getSplitOrientation() {
            return splitOrientation;
        }
    }
}
