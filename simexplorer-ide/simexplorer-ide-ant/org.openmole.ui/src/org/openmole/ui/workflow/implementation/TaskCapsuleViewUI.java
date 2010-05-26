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
import org.netbeans.api.visual.layout.LayoutFactory;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.ui.workflow.model.IObjectModelUI;
import org.openmole.ui.workflow.model.IGenericTaskModelUI;
import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.workflow.implementation.Preferences.Settings;
import org.openmole.ui.workflow.implementation.paint.MyConnectableWidget;
import org.openmole.ui.workflow.implementation.paint.MyWidget;
import org.openmole.ui.workflow.model.ICapsuleModelUI;
import org.openmole.ui.workflow.model.ITaskCapsuleView;
import org.openmole.ui.workflow.provider.DnDAddPrototypeProvider;
import org.openmole.ui.workflow.provider.DnDNewTaskProvider;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskCapsuleViewUI extends ObjectViewUI implements ITaskCapsuleView {

    private IGenericTaskModelUI<IGenericTask> taskModel = null;
    protected MyConnectableWidget connectableWidget;
    protected ICapsuleModelUI capsuleModel;
    private DnDAddPrototypeProvider dnDAddPrototypeProvider;

    public TaskCapsuleViewUI(MoleScene scene,
            ICapsuleModelUI tcm) {

        super(scene,
                Preferences.getInstance().getCapsuleModelSettings().getDefaultBackgroundColor(),
                Preferences.getInstance().getCapsuleModelSettings().getDefaultBorderColor());
        capsuleModel = tcm;

        ApplicationCustomize colorCustomize = ApplicationCustomize.getInstance();

        connectableWidget = new MyConnectableWidget(scene,
                getBackgroundColor(),
                getBorderColor(),
                getBackgroundImage());
        setLayout(LayoutFactory.createVerticalFlowLayout());
        addChild(connectableWidget);

        addInputSlot();
        addOutputSlot();

        dnDAddPrototypeProvider = new DnDAddPrototypeProvider(scene, this);

        getActions().addAction(ActionFactory.createPopupMenuAction(new TaskCapsuleMenuProvider(scene, this)));
        getActions().addAction(ActionFactory.createAcceptAction(new DnDNewTaskProvider(scene, this)));
        getActions().addAction(ActionFactory.createAcceptAction(dnDAddPrototypeProvider));

    }

    public IGenericTaskModelUI<IGenericTask> getTaskModel() {
        return taskModel;
    }

    public void encapsule(Class<? extends IGenericTask> coreTaskClass) {
        this.taskModel = UIFactory.getInstance().createTaskModelInstance((Class<? extends IGenericTaskModelUI>) Preferences.getInstance().getModelClass(coreTaskClass));
        this.taskModel.setTask(UIFactory.getInstance().createCoreTaskInstance(coreTaskClass));

        Settings sets = Preferences.getInstance().getModelSettings((Class<? extends IObjectModelUI>) taskModel.getClass());
        connectableWidget.setBackgroundCol(sets.getDefaultBackgroundColor());
        connectableWidget.setBorderCol(sets.getDefaultBorderColor());
        connectableWidget.setBackgroundImaqe(sets.getDefaultBackgroundImage());

        dnDAddPrototypeProvider.setEncapsulated(true);

        scene.getManager().incrementNodeName();
        connectableWidget.addTitle(scene.getManager().getNodeName());


        GenericMenuProvider gmp = PopupMenuProviderFactory.merge(new TaskMenuProvider(taskModel),
                new TaskCapsuleMenuProvider(scene, this),
                capsuleModel);

        getActions().addAction(ActionFactory.createPopupMenuAction(gmp));
        getActions().addAction(new TaskActions(taskModel, this));
    }

    @Override
    public void setTaskCapsule(IGenericTaskCapsule tc) {
        capsuleModel.setTaskCapsule(tc);
    }

    @Override
    public void addInputSlot() {
        capsuleModel.addInputSlot();
        connectableWidget.addInputSlot();
    }

    @Override
    public void addOutputSlot() {
        capsuleModel.addOutputSlot();
        connectableWidget.addOutputSlot();
    }

    @Override
    public MyConnectableWidget getConnectableWidget() {
        return connectableWidget;
    }

    @Override
    public ICapsuleModelUI getTaskCapsuleModel() {
        return capsuleModel;
    }

    @Override
    public IObjectModelUI getModel() {
        return (IObjectModelUI) capsuleModel;
    }

    @Override
    public MyWidget getWidget() {
        return connectableWidget;
    }
}
