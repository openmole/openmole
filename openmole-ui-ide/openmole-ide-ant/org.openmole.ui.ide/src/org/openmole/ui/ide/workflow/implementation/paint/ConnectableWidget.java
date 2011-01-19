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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.widget.ImageWidget;
import org.openmole.ui.ide.commons.ApplicationCustomize;
import org.openmole.ui.ide.control.MoleScenesManager;
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

    private IGenericTaskModelUI<IGenericTask> taskModel = TaskModelUI.EMPTY_TASK_MODEL;
    List<ISlotWidget> islots = new ArrayList<ISlotWidget>();
    OSlotWidget oslot;

    //private WidgetAction mouseHoverAction = ActionFactory.createHoverAction(new ImageHoverProvider());
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
    }

    public ConnectableWidget(MoleScene scene,
            ICapsuleModelUI capsuleModel,
            Color backgroundCol,
            Color borderCol,
            Image backgroundImaqe,
            IGenericTaskModelUI taskModelUI) {
        this(scene, capsuleModel, backgroundCol, borderCol, backgroundImaqe);
        this.taskModel = taskModelUI;
        createActions(MoleScene.MOVE).addAction(ActionFactory.createMoveAction());
    }

    public void setTaskModel(IGenericTaskModelUI<IGenericTask> taskModelUI) {
        this.taskModel = taskModelUI;
    }

    public void setDetailedView() {
        setWidthHint();
        oslot.setDetailedView(taskWidth);
    }

    public void addInputSlot(ISlotWidget iw) {
        islots.add(iw);
        addChild(iw);
    }

    public List<ISlotWidget> getIslots() {
        return islots;
    }

    public void addOutputSlot(OSlotWidget ow) {
        addChild(ow);
        oslot = ow;
    }

    public void clearInputSlots() {
        for (ImageWidget iw : islots) {
            removeChild(iw);
        }
        islots.clear();
    }

    @Override
    protected void paintWidget() {
        super.paintWidget();
        Graphics2D graphics = getGraphics();

        graphics.setColor(borderCol);
        BasicStroke stroke = new BasicStroke(1.3f, 1, 1);
        graphics.draw(stroke.createStrokedShape(bodyArea));

        if (taskModel != TaskModelUI.EMPTY_TASK_MODEL) {
            graphics.drawLine(taskWidth / 2,
                    ApplicationCustomize.TASK_TITLE_HEIGHT,
                    taskWidth / 2,
                    widgetArea.height - 3);

            graphics.setColor(new Color(0, 0, 0));
            int x = taskWidth / 2 + 9;

            List<Set<PrototypeUI>> li = new ArrayList<Set<PrototypeUI>>();
            li.add(taskModel.getPrototypesIn());
            li.add(taskModel.getPrototypesOut());
            int h = 0;
            for (Set<PrototypeUI> protoIO : li) {
                int i = 0;
                for (PrototypeUI proto : protoIO) {
                    String st = proto.getName();
                    if (st.length() > 10) {
                        st = st.substring(0, 8).concat("...");
                    }
                    h = 35 + i * 22;
                    graphics.drawImage(ApplicationCustomize.getInstance().getTypeImage(proto.getType().getSimpleName()),
                            x - taskWidth / 2, h - 13,
                            new Container());
                    AttributedString as = new AttributedString(st);
                    if (MoleScenesManager.getInstance().isDetailedView()) {
                        graphics.drawString(as.getIterator(), x - taskWidth / 2 + 24, h);
                    }
                    i++;
                }
                x += taskWidth / 2 - 1;
            }

            int newH = Math.max(taskModel.getPrototypesIn().size(), taskModel.getPrototypesOut().size()) * 22 + 45;
            int delta = bodyArea.height - newH;
            if (delta < 0) {
                bodyArea.setSize(bodyArea.width, newH);
                enlargeWidgetArea(0, -delta);
            }
        }
        revalidate();
    }
}
