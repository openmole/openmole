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
package org.openmole.ui.workflow.implementation;

import org.netbeans.api.visual.layout.LayoutFactory;
import org.openmole.ui.workflow.implementation.paint.MyWidget;
import org.openmole.ui.workflow.model.IGenericTaskModelUI;
import org.openmole.ui.workflow.model.IObjectModelUI;
import org.openmole.ui.workflow.model.ITaskViewUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskViewUI extends ObjectViewUI implements ITaskViewUI{
    protected IGenericTaskModelUI model;
    private MyWidget widget;

    public TaskViewUI(MoleScene scene,
            IGenericTaskModelUI m,
            String n) {
        super(scene,
                Preferences.getInstance().getModelSettings((Class<? extends IObjectModelUI>) m.getClass()).getDefaultBackgroundColor(),
                Preferences.getInstance().getModelSettings((Class<? extends IObjectModelUI>) m.getClass()).getDefaultBorderColor(),
                Preferences.getInstance().getModelSettings((Class<? extends IObjectModelUI>) m.getClass()).getDefaultBackgroundImage());

        model = m;
        widget = new MyWidget(scene,
                getBackgroundColor(),
                getBorderColor(),
                getBackgroundImage());
        widget.addTitle(n);

        setLayout(LayoutFactory.createVerticalFlowLayout());
        addChild(widget);

        getActions().addAction(new TaskActions(model,this));
    }

    @Override
    public IObjectModelUI getModel() {
        return (IObjectModelUI) model;
    }

    @Override
    public MyWidget getWidget() {
        return widget;
    }
}
