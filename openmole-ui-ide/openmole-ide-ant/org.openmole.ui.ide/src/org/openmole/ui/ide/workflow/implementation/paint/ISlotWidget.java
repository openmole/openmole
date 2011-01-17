/*
 * Copyright (C) 2011 Mathieu Leclaire <mathieu.leclaire@openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ui.ide.workflow.implementation.paint;

import java.awt.Point;
import org.netbeans.api.visual.widget.ImageWidget;
import org.netbeans.api.visual.widget.Scene;
import org.openmole.ui.ide.commons.ApplicationCustomize;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class ISlotWidget extends ImageWidget {
    private int index;
    private boolean startingSlot;

    public ISlotWidget(Scene scene,
            int index,
            boolean startingSlot) {
        super(scene);
        this.index = index;
        this.startingSlot = startingSlot;
        setImage(startingSlot ? ApplicationCustomize.IMAGE_START_SLOT : ApplicationCustomize.IMAGE_INPUT_SLOT);
        setPreferredLocation(new Point(-12, 14 + index * 20));
    }

    public boolean isStartingSlot() {
        return startingSlot;
    }

    public int getIndex() {
        return index;
    }
}
