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

import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.ui.ide.workflow.model.IUIFactory;
import org.openmole.commons.tools.object.Instanciator;
import org.openmole.ui.ide.exception.MoleExceptionManagement;
import org.openmole.ui.ide.palette.Category.CategoryName;
import org.openmole.ui.ide.workflow.model.ICapsuleView;
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
    public IGenericTaskModelUI createTaskModelInstance(Class<? extends IGenericTaskModelUI> modelClass,
            TaskUI task) throws UserBadDataError {
        try {
            return Instanciator.instanciate(modelClass, task);
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

    public void createCapsule(MoleScene scene) {
        createCapsule(scene, new Point(0, 0));
    }

    public ICapsuleView createCapsule(MoleScene scene,
            Point locationPoint) {
        ICapsuleView obUI = null;
        try {
            obUI = new CapsuleViewUI(scene,
                    new CapsuleModelUI(),
                    Preferences.getInstance().getProperties(CategoryName.CAPSULE,
                    org.openmole.core.implementation.capsule.Capsule.class));
        } catch (UserBadDataError ex) {
            MoleExceptionManagement.showException(ex);
        }

        scene.initCapsuleAdd(obUI);
        scene.getManager().registerCapsuleView(obUI);
        scene.addNode(scene.getManager().getNodeID()).setPreferredLocation(locationPoint);
        return obUI;
    }

    public static UIFactory getInstance() {
        if (instance == null) {
            instance = new UIFactory();
        }
        return instance;
    }

    @Override
    public void eventOccured(Object t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
