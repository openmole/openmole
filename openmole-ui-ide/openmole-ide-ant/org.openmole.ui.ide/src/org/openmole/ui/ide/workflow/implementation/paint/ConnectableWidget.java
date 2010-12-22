/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.anchor.Anchor;
import org.openmole.ui.ide.commons.ApplicationCustomize;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;
import org.openmole.ui.ide.workflow.implementation.TaskModelUI;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class ConnectableWidget extends MyWidget {

    private int inputDelta = 0;
    private int outputDelta = 0;
    private IGenericTaskModelUI<IGenericTask> taskModel = TaskModelUI.EMPTY_TASK_MODEL;
    private ICapsuleModelUI capsuleModel;

    
    public ConnectableWidget(MoleScene scene,
            ICapsuleModelUI capsuleModel,
            Color backgroundCol,
            Color borderCol,
            Image img) {
        super(scene,
                backgroundCol,
                borderCol,
                img);
        this.borderCol = borderCol;
        this.capsuleModel = capsuleModel;
    }

    public ConnectableWidget(MoleScene scene,
            ICapsuleModelUI capsuleModel,
            Color backgroundCol,
            Color borderCol,
            Image backgroundImaqe,
            IGenericTaskModelUI taskModelUI) {
        this(scene, capsuleModel,backgroundCol, borderCol, backgroundImaqe);
        this.taskModel = taskModelUI;
        createActions(MoleScene.MOVE).addAction (ActionFactory.createMoveAction());
    }

    public void setTaskModel(IGenericTaskModelUI<IGenericTask> taskModelUI) {
        this.taskModel = taskModelUI;
    }

    public void setDetailedView() {
        setWidthHint();
    }

    public void adjustInputSlotPosition() {
        inputDelta = (int) (ApplicationCustomize.TASK_CONTAINER_HEIGHT - ApplicationCustomize.TASK_TITLE_HEIGHT - capsuleModel.getNbInputslots() * 14) / (capsuleModel.getNbInputslots() + 1);
        repaint();
    }

    public void adjustOutputSlotPosition() {
        outputDelta = (int) (ApplicationCustomize.TASK_CONTAINER_HEIGHT - ApplicationCustomize.TASK_TITLE_HEIGHT - capsuleModel.getNbOutputslots() * 14) / (capsuleModel.getNbOutputslots() + 1);
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

        adjustInputSlotPosition();
        graphics.setColor(borderCol);
        BasicStroke stroke = new BasicStroke(1.3f, 1, 1);
        graphics.draw(stroke.createStrokedShape(bodyArea));

        for (int i = 0; i < capsuleModel.getNbInputslots(); ++i) {
            graphics.drawImage(capsuleModel.isStartingCapsule() ? ApplicationCustomize.IMAGE_START_SLOT : ApplicationCustomize.IMAGE_INPUT_SLOT,
                    -8,
                    ApplicationCustomize.TASK_TITLE_HEIGHT + i * (inputDelta + 14) + inputDelta,
                    new Container());
        }

        for (int i = 0; i < capsuleModel.getNbOutputslots(); ++i) {
            graphics.drawImage(ApplicationCustomize.IMAGE_OUTPUT_SLOT,
                    taskWidth - 8,
                    ApplicationCustomize.TASK_TITLE_HEIGHT + i * (outputDelta + 14) + outputDelta,
                    new Container());
        }

        if (scene.isDetailedView()) {
            if (taskModel != TaskModelUI.EMPTY_TASK_MODEL)
                graphics.drawLine(taskWidth / 2,
                        ApplicationCustomize.TASK_TITLE_HEIGHT,
                        taskWidth / 2,
                        ApplicationCustomize.TASK_CONTAINER_HEIGHT);

            graphics.setColor(new Color(0, 0, 0));
            int x = taskWidth / 2 + 9;

            List<Set<PrototypeUI>> li = new ArrayList<Set<PrototypeUI>>();
            li.add(taskModel.getPrototypesIn());
            li.add(taskModel.getPrototypesOut());
            int varlenght = 0;
            for (Set<PrototypeUI> protoIO : li) {
                int i = 0;
                for (PrototypeUI proto : protoIO) {
                    String st = proto.getName();
                    if (st.length() > 10) {
                        st = st.substring(0, 10).concat("...");
                    }
                    varlenght = st.length();
                    st += " : " + proto.getType().getSimpleName();
                    AttributedString as = new AttributedString(st);
                    as.addAttribute(TextAttribute.FOREGROUND, Color.GRAY, varlenght, st.length());
                    graphics.drawString(as.getIterator(), x - taskWidth / 2, 35 + i * 15);
                    i++;
                }
                x += taskWidth / 2 - 1;
            }

            int newH = Math.max(taskModel.getPrototypesIn().size(), taskModel.getPrototypesOut().size()) * 15 + 45;
            int delta = bodyArea.height - newH;
            if (delta < 0) {
                bodyArea.setSize(bodyArea.width, newH);
                enlargeWidgetArea(0, -delta);
            }
        }
        revalidate();
    }
}
