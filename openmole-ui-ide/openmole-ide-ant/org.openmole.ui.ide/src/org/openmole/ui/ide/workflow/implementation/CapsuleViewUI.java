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
package org.openmole.ui.ide.workflow.implementation;

import java.util.Properties;
import org.openmole.ui.ide.workflow.provider.TaskCapsuleMenuProvider;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.ui.ide.workflow.model.IObjectModelUI;
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.ui.ide.palette.Category.CategoryName;
import org.openmole.ui.ide.workflow.implementation.paint.ConnectableWidget;
import org.openmole.ui.ide.workflow.implementation.paint.MyWidget;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;
import org.openmole.ui.ide.workflow.model.ICapsuleView;
import org.openmole.ui.ide.workflow.provider.DnDAddPrototypeInstanceProvider;
import org.openmole.ui.ide.workflow.provider.DnDNewTaskProvider;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class CapsuleViewUI extends ObjectViewUI implements ICapsuleView {

    private IGenericTaskModelUI<IGenericTask> taskModel = TaskModelUI.EMPTY_TASK_MODEL;
    protected ConnectableWidget connectableWidget;
    protected ICapsuleModelUI capsuleModel;
    private DnDAddPrototypeInstanceProvider dnDAddPrototypeInstanceProvider;
    private TaskCapsuleMenuProvider taskCapsuleMenuProvider;

    
    public CapsuleViewUI(MoleScene scene,
            ICapsuleModelUI tcm,
            Properties properties) {

        super(scene, properties);
        capsuleModel = tcm;

        connectableWidget = new ConnectableWidget(scene,
                capsuleModel,
                getBackgroundColor(),
                getBorderColor(),
                getBackgroundImage());
        setLayout(LayoutFactory.createVerticalFlowLayout());
        addChild(connectableWidget);

        addInputSlot();
        addOutputSlot();

        dnDAddPrototypeInstanceProvider = new DnDAddPrototypeInstanceProvider(scene, this);

        taskCapsuleMenuProvider = new TaskCapsuleMenuProvider(scene, this);
        getActions().addAction(ActionFactory.createPopupMenuAction(taskCapsuleMenuProvider));
        getActions().addAction(ActionFactory.createAcceptAction(new DnDNewTaskProvider(scene, this)));
        getActions().addAction(ActionFactory.createAcceptAction(dnDAddPrototypeInstanceProvider));

    }

    @Override
    public String getName() {
        return connectableWidget.getTitleString();
    }
    
    public IGenericTaskModelUI<IGenericTask> getTaskModel() {
        return taskModel;
    }

    public void encapsule(Class<? extends IGenericTask> coreTaskClass) throws UserBadDataError {
        this.taskModel = UIFactory.getInstance().createTaskModelInstance((Class<? extends IGenericTaskModelUI>) Preferences.getInstance().getModel(CategoryName.TASK, coreTaskClass));
        this.taskModel.setCoreTaskClass(coreTaskClass);

        properties = Preferences.getInstance().getProperties(CategoryName.TASK, coreTaskClass);

        changeConnectableWidget();

        dnDAddPrototypeInstanceProvider.setEncapsulated(true);

        scene.getManager().incrementNodeName();
        connectableWidget.addTitle(scene.getManager().getNodeName());

        taskCapsuleMenuProvider.addTaskMenus();
        getActions().addAction(new TaskActions(taskModel, this));
    }

    @Override
    public void changeConnectableWidget(){
        connectableWidget.setBackgroundCol(getBackgroundColor());
        connectableWidget.setBorderCol(getBorderColor());
        connectableWidget.setBackgroundImaqe(getBackgroundImage());
        connectableWidget.setTaskModel(taskModel);
    }
    
    @Override
    public void addInputSlot() {
        capsuleModel.addInputSlot();
        connectableWidget.adjustInputSlotPosition();
    }

    @Override
    public void addOutputSlot() {
        capsuleModel.addOutputSlot();
        connectableWidget.adjustOutputSlotPosition();
    }

    @Override
    public ConnectableWidget getConnectableWidget() {
        return connectableWidget;
    }

    @Override
    public ICapsuleModelUI getCapsuleModel() {
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
