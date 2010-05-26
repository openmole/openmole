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

import java.awt.Point;
import org.openide.util.Exceptions;
import java.lang.reflect.InvocationTargetException;
import org.netbeans.api.visual.widget.Widget;
import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.ui.workflow.model.IUIFactory;
import org.openmole.misc.tools.object.Instanciator;
import org.openmole.ui.exception.MoleExceptionManagement;
import org.openmole.ui.workflow.model.ITaskCapsuleView;
import org.openmole.ui.workflow.model.IGenericTaskModelUI;

/**
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 * Singleton class providing a createTaskModel method to build new UI instance
 * according to the core Object given as argument. A Mapping between
 * core instances and UI classes is given by the businessToUIMap hash map.
 */
public class UIFactory implements IUIFactory<Object> {

    private static UIFactory instance = null;

    /*   @Override
    public ICapsuleModelUI createTaskCapsuleModel(IGenericTaskCapsule gtc) {
    try {
    return Instanciator.instanciate(Preferences.getInstance().getCapsuleMapping(gtc.getClass()));
    } catch (IllegalArgumentException illE) {
    MoleExceptionManagement.showException(illE);
    } catch (NoSuchMethodException methE) {
    MoleExceptionManagement.showException(methE);
    } catch (InstantiationException instE) {
    MoleExceptionManagement.showException(instE);
    } catch (IllegalAccessException accessE) {
    MoleExceptionManagement.showException(accessE);
    } catch (InvocationTargetException invokE) {
    MoleExceptionManagement.showException(invokE);
    }
    return null;
    }*/
    public IGenericTask createCoreTaskInstance(Class<? extends IGenericTask> taskClass) {
        try {
            return Instanciator.instanciate(taskClass);
        } catch (IllegalArgumentException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (NoSuchMethodException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (InstantiationException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (IllegalAccessException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (InvocationTargetException ex) {
            MoleExceptionManagement.showException(ex);
        }
        return null;
    }

    @Override
    public IGenericTaskModelUI createTaskModelInstance(Class<? extends IGenericTaskModelUI> modelClass) {
        try {
            return Instanciator.instanciate(modelClass);
        } catch (IllegalArgumentException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (NoSuchMethodException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (InstantiationException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (IllegalAccessException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (InvocationTargetException ex) {
            MoleExceptionManagement.showException(ex);
        }
        return null;
    }

    public ITaskCapsuleView createTaskCapsule(MoleScene scene) {
        return createTaskCapsule(scene, new Point(0, 0));
    }

    public ITaskCapsuleView createTaskCapsule(MoleScene scene,
            Point locationPoint) {
        TaskCapsuleModelUI tcm = new TaskCapsuleModelUI();
        Widget obUI = new TaskCapsuleViewUI(scene, tcm);
        scene.initCapsuleAdd(obUI);
        scene.addNode(scene.getManager().getNodeID()).setPreferredLocation(locationPoint);
        scene.getManager().registerTaskCapsuleModel(tcm);

        scene.getManager().printTaskC();

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
