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
package org.openmole.ui.ide.workflow.implementation;

import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import org.netbeans.api.visual.widget.Widget;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.ui.ide.workflow.model.IUIFactory;
import org.openmole.commons.tools.object.Instanciator;
import org.openmole.ui.ide.exception.MoleExceptionManagement;
import org.openmole.ui.ide.palette.Category.CategoryName;
import org.openmole.ui.ide.workflow.model.ITaskCapsuleView;
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI;

/**
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 * Singleton class providing a createTaskModel method to build new UI instance
 * according to the core Object given as argument. A Mapping between
 * core instances and UI classes is given by the businessToUIMap hash map.
 */
public class UIFactory implements IUIFactory<Object> {

    private static UIFactory instance = null;


    @Override
    public IGenericTask createCoreTaskInstance(Class<? extends IGenericTask> taskClass) throws UserBadDataError {
        try {
            return Instanciator.instanciate(taskClass);
        } catch (IllegalArgumentException ex) {
            throw new UserBadDataError(ex);
        } catch (NoSuchMethodException ex) {
            throw new UserBadDataError(ex);
        } catch (InstantiationException ex) {
            throw new UserBadDataError(ex);
        } catch (IllegalAccessException ex) {
            throw new UserBadDataError(ex);
        } catch (InvocationTargetException ex) {
            throw new UserBadDataError(ex);
        }
    }

    @Override
    public IGenericTaskModelUI createTaskModelInstance(Class<? extends IGenericTaskModelUI> modelClass) throws UserBadDataError {
        try {
            return Instanciator.instanciate(modelClass);
        } catch (IllegalArgumentException ex) {
            throw new UserBadDataError(ex);
        } catch (NoSuchMethodException ex) {
            throw new UserBadDataError(ex);
        } catch (InstantiationException ex) {
            throw new UserBadDataError(ex);
        } catch (IllegalAccessException ex) {
            throw new UserBadDataError(ex);
        } catch (InvocationTargetException ex) {
            throw new UserBadDataError(ex);
        }
    }

    public ITaskCapsuleView createTaskCapsule(MoleScene scene) {
            return createTaskCapsule(scene, new Point(0, 0));
    }

    public ITaskCapsuleView createTaskCapsule(MoleScene scene,
                                              Point locationPoint){
        TaskCapsuleModelUI tcm = new TaskCapsuleModelUI();
        Widget obUI= null;
        try {
            obUI = new TaskCapsuleViewUI(scene, tcm, Preferences.getInstance().getProperties(CategoryName.TASK_CAPSULE, org.openmole.core.implementation.capsule.TaskCapsule.class));
        } catch (UserBadDataError ex) {
            MoleExceptionManagement.showException(ex);
        }
        scene.initCapsuleAdd(obUI);
        scene.addNode(scene.getManager().getNodeID()).setPreferredLocation(locationPoint);
        scene.getManager().registerTaskView((ITaskCapsuleView) obUI);


        return (ITaskCapsuleView) obUI;
    }

    @Override
    public void objectConstructed(Object t) {
        //   ServiceProxy.getEventDispatcher().registerListner(obj,createTaskModel(obj));
    }

    public static UIFactory getInstance() {
        if (instance == null) {
            instance = new UIFactory();
        }
        return instance;
    }
}
