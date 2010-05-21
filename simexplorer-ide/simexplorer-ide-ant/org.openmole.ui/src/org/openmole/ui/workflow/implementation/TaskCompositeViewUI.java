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

import org.openmole.ui.workflow.provider.PopupMenuProviderFactory;
import org.openmole.ui.workflow.provider.TaskMenuProvider;
import org.openmole.ui.workflow.provider.GenericMenuProvider;
import org.openmole.ui.workflow.provider.TaskCapsuleMenuProvider;
import org.netbeans.api.visual.action.ActionFactory;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.workflow.implementation.paint.MyConnectableWidget;
import org.openmole.ui.workflow.model.IObjectModelUI;
import org.openmole.ui.workflow.model.IGenericTaskModelUI;
import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.ui.workflow.model.ICapsuleModelUI;
import org.openmole.ui.workflow.model.ITaskCapsuleModelUI;
import org.openmole.ui.workflow.model.ITaskViewUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskCompositeViewUI extends Connectable implements ITaskViewUI{

    private IGenericTaskModelUI<IGenericTask> taskModel;
    private IObjectModelUI model;

    public TaskCompositeViewUI(MoleScene scene,
            ICapsuleModelUI tcm,
            IGenericTaskModelUI<IGenericTask> m,
            String name) {
        super(scene,
                tcm,
                Preferences.getInstance().getModelSettings(m.getClass()).getDefaultBackgroundColor(),
                Preferences.getInstance().getModelSettings(m.getClass()).getDefaultBorderColor(),
                Preferences.getInstance().getModelSettings(m.getClass()).getDefaultBackgroundImage(),
                name);

        taskModel = m;

        ApplicationCustomize colorCustomize = ApplicationCustomize.getInstance();
        scene.getManager().registerTaskModel(name, m);

        connectableWidget = new MyConnectableWidget(scene,
                getBackgroundColor(),
                getBorderColor(),
                getBackgroundImage());


        connectableWidget.addTitle(name);
        addChild(connectableWidget);

        GenericMenuProvider gmp = PopupMenuProviderFactory.merge(new TaskMenuProvider(taskModel),
                new TaskCapsuleMenuProvider(this),
                model);

        getActions().addAction(ActionFactory.createPopupMenuAction(gmp));
      //  getActions().addAction(new TaskActions(taskModel, this));
    }
}
