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
import org.openmole.ui.ide.workflow.provider.CapsuleMenuProvider;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.ui.ide.commons.ApplicationCustomize;
import org.openmole.ui.ide.workflow.model.IObjectModelUI;
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI;
import org.openmole.ui.ide.control.MoleScenesManager;
import org.openmole.ui.ide.palette.Category.CategoryName;
import org.openmole.ui.ide.workflow.implementation.paint.ConnectableWidget;
import org.openmole.ui.ide.workflow.implementation.paint.ISlotWidget;
import org.openmole.ui.ide.workflow.implementation.paint.MyWidget;
import org.openmole.ui.ide.workflow.implementation.paint.OSlotWidget;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;
import org.openmole.ui.ide.workflow.model.ICapsuleView;
import org.openmole.ui.ide.workflow.provider.DnDAddPrototypeProvider;
import org.openmole.ui.ide.workflow.provider.DnDNewTaskProvider;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class CapsuleViewUI extends ObjectViewUI implements ICapsuleView {

    protected ConnectableWidget connectableWidget;
    protected ICapsuleModelUI capsuleModel;
    private DnDAddPrototypeProvider dnDAddPrototypeInstanceProvider;
    private CapsuleMenuProvider taskCapsuleMenuProvider;

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

        //Default input slot
        addInputSlot();

        //Default output slot
        connectableWidget.addOutputSlot(new OSlotWidget(scene));

        dnDAddPrototypeInstanceProvider = new DnDAddPrototypeProvider(scene, this);

        taskCapsuleMenuProvider = new CapsuleMenuProvider(scene, this);
        getActions().addAction(ActionFactory.createPopupMenuAction(taskCapsuleMenuProvider));
        getActions().addAction(ActionFactory.createAcceptAction(new DnDNewTaskProvider(scene, this)));
        getActions().addAction(ActionFactory.createAcceptAction(dnDAddPrototypeInstanceProvider));

    }
    public void defineAsRegularCapsule(){
        capsuleModel.defineAsRegularCapsule();
        connectableWidget.clearInputSlots();
        connectableWidget.addInputSlot(new ISlotWidget(scene,1,false));
    }

    public void defineAsStartingCapsule(){
        capsuleModel.defineAsStartingCapsule();
        connectableWidget.clearInputSlots();
        connectableWidget.addInputSlot(new ISlotWidget(scene,1,true));
    }

    @Override
    public void encapsule(TaskUI taskUI) throws UserBadDataError {
        capsuleModel.setTaskModel(UIFactory.getInstance().createTaskModelInstance((Class<? extends IGenericTaskModelUI>) Preferences.getInstance().getModel(CategoryName.TASK, taskUI.getType()), taskUI));


        properties = Preferences.getInstance().getProperties(CategoryName.TASK, taskUI.getType());

        changeConnectableWidget();

        dnDAddPrototypeInstanceProvider.setEncapsulated(true);

        MoleScenesManager.getInstance().incrementNodeName();
        connectableWidget.addTitle(taskUI.getName());

        taskCapsuleMenuProvider.addTaskMenus();
        getActions().addAction(new TaskActions(capsuleModel.getTaskModel(), this));
    }

    @Override
    public void changeConnectableWidget() {
        connectableWidget.setBackgroundCol(getBackgroundColor());
        connectableWidget.setBorderCol(getBorderColor());
        connectableWidget.setBackgroundImaqe(getBackgroundImage());
        connectableWidget.setTaskModel(capsuleModel.getTaskModel());
    }

    @Override
    public void addInputSlot() {
        capsuleModel.addInputSlot();
        ISlotWidget im = new ISlotWidget(scene, getCapsuleModel().getNbInputslots(),capsuleModel.isStartingCapsule() ? true : false);
        getConnectableWidget().addInputSlot(im);
        scene.refresh();
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
