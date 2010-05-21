/*
 *  Copyright (C) 2010 leclaire
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
package org.openmole.ui.workflow.implementation.paint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import org.netbeans.api.visual.anchor.Anchor;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.workflow.implementation.MoleScene;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MyConnectableWidget extends MyWidget {

    private int nbInSlot = 0;
    private int nbOutSlot = 0;
    private int inputDelta = 0;
    private int outputDelta = 0;

    public MyConnectableWidget(MoleScene scene,
            Color backgroundCol,
            Color borderCol) {
        super(scene,
                backgroundCol);
    }

    public MyConnectableWidget(MoleScene scene,
            Color col) {
        super(scene,
                col);
    }

    public MyConnectableWidget(MoleScene scene,
            Color backgroundCol,
            Color borderCol,
            Image img) {
        super(scene,
                backgroundCol,
                borderCol,
                img);
        this.borderCol = borderCol;
    }

    public void addInputSlot() {
        nbInSlot++;
        inputDelta = (int) (ApplicationCustomize.TASK_CONTAINER_HEIGHT - ApplicationCustomize.TASK_TITLE_HEIGHT - nbInSlot * 14) / (nbInSlot + 1);
        repaint();
    }

    public void addOutputSlot() {
        nbOutSlot++;
        outputDelta = (int) (ApplicationCustomize.TASK_CONTAINER_HEIGHT - ApplicationCustomize.TASK_TITLE_HEIGHT - nbOutSlot * 14) / (nbOutSlot + 1);
        repaint();
    }

    public Anchor getInputSlotAnchor(int index) {
        return new MyAnchor(this,
                IOType.INPUT,
                index);
    }

    public Anchor getOutputSlotAnchor(int index) {
        return new MyAnchor(this,
                IOType.OUTPUT,
                index);
    }

    public Point getInputSlotPoint(int index) {
        return new Point(0,
                ApplicationCustomize.TASK_TITLE_HEIGHT + (inputDelta + 7) * (index + 1));
    }

    public Point getOutputSlotPoint(int index) {
        return new Point(ApplicationCustomize.TASK_CONTAINER_WIDTH + 8,
                ApplicationCustomize.TASK_TITLE_HEIGHT + (outputDelta + 7) * (index + 1));
    }

    @Override
    protected void paintWidget() {
        super.paintWidget();
        Graphics2D graphics = getGraphics();

        graphics.setColor(borderCol);
        BasicStroke stroke = new BasicStroke(1.3f, 1, 1);
        graphics.draw(stroke.createStrokedShape(bodyArea));

        for (int i = 0; i < nbInSlot; ++i) {
            graphics.drawImage(ApplicationCustomize.IMAGE_INPUT_SLOT,
                    -8,
                    ApplicationCustomize.TASK_TITLE_HEIGHT + inputDelta * (i + 1) + 14 * i,
                    new Container());
        }

        for (int i = 0; i < nbOutSlot; ++i) {
            graphics.drawImage(ApplicationCustomize.IMAGE_OUTPUT_SLOT,
                    ApplicationCustomize.TASK_CONTAINER_WIDTH - 8,
                    ApplicationCustomize.TASK_TITLE_HEIGHT + outputDelta * (i + 1) + 14 * i,
                    new Container());
        }
    }
}
