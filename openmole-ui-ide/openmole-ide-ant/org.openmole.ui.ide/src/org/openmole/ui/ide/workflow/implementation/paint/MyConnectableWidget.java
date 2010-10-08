/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.ui.ide.workflow.implementation.paint;

import org.openmole.core.model.task.IGenericTask;
import org.openmole.ui.ide.commons.IOType;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import org.netbeans.api.visual.anchor.Anchor;
import org.openmole.ui.ide.commons.ApplicationCustomize;
import org.openmole.ui.ide.workflow.implementation.GenericTaskModelUI;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;
import org.openmole.ui.ide.workflow.implementation.TaskModelUI;
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MyConnectableWidget extends MyWidget {

    private int nbInSlot = 0;
    private int nbOutSlot = 0;
    private int inputDelta = 0;
    private int outputDelta = 0;
    private IGenericTaskModelUI<IGenericTask>  taskModel = TaskModelUI.EMPTY_TASK_MODEL;

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

    public MyConnectableWidget(MoleScene scene,
                               Color backgroundCol,
                               Color borderCol,
                               Image backgroundImaqe,
                               IGenericTaskModelUI taskModelUI) {
        this(scene, backgroundCol, borderCol, backgroundImaqe);
        this.taskModel = taskModelUI;
    }

    public void setTaskModel(IGenericTaskModelUI<IGenericTask> taskModelUI){
        this.taskModel = taskModelUI;
    }

    public void setDetailedView() {
        setWidthHint();
        //  taskWidth = ApplicationCustomize.EXPANDED_TASK_CONTAINER_WIDTH;
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
        return new Point(taskWidth + 8,
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
                    taskWidth - 8,
                    ApplicationCustomize.TASK_TITLE_HEIGHT + outputDelta * (i + 1) + 14 * i,
                    new Container());
        }

        if (scene.isDetailedView()) {
            graphics.setColor(new Color(0, 0, 0));
            int x = taskWidth - 20;

            FontMetrics fm = graphics.getFontMetrics(graphics.getFont());
            int i = 0;
            for (PrototypeUI proto : taskModel.getPrototypesIn()) {
                graphics.drawString(proto.getName(), 20, 35 + i * 15);
                i++;
            }

            int j = 0;
            for (PrototypeUI proto : taskModel.getPrototypesOut()) {
                graphics.drawString(proto.getName(), x - fm.stringWidth(proto.getName()), 35 + j * 15);
                j++;
            }

            int newH = Math.max(i, j) * 15 + 45;
            int delta = bodyArea.height - newH;
            if (delta < 0) {
                bodyArea.setSize(bodyArea.width, newH);
                enlargeWidgetArea(0, -delta);
            }
        }
        revalidate();
    }
}
