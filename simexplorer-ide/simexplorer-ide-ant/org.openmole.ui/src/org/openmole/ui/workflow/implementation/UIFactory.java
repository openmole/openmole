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

import org.openide.util.Exceptions;
import org.openmole.ui.exception.MoleExceptionManagement;
import java.lang.reflect.InvocationTargetException;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.ui.workflow.model.IUIFactory;
import org.openmole.commons.tools.object.Instanciator;
import org.openmole.ui.workflow.model.ICapsuleModelUI;
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

    @Override
    public ICapsuleModelUI createTaskCapsuleModel(IGenericTaskCapsule gtc) {
        try {
            return Instanciator.instanciate(Preferences.getInstance().getCapsuleMapping(gtc.getClass()));
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InstantiationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }


    @Override
    public <T> IGenericTaskModelUI createTaskModel(T obj) {
                try {
                    return Instanciator.instanciate(Preferences.getInstance().getModelClass(obj.getClass()));
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
    }

    @Override
    public void objectConstructed(Object obj){
     //   ServiceProxy.getEventDispatcher().registerListner(obj,createTaskModel(obj));
    }

    public static UIFactory getInstance() {
        if (instance == null) {
            instance = new UIFactory();
        }
        return instance;
    }
}
