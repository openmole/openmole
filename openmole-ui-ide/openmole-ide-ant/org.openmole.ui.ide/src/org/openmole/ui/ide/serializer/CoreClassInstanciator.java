/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.org>
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
package org.openmole.ui.ide.serializer;

import java.lang.reflect.InvocationTargetException;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.object.Instanciator;
import org.openmole.core.implementation.capsule.Capsule;
import org.openmole.core.implementation.capsule.ExplorationCapsule;
import org.openmole.core.model.capsule.IGenericCapsule;
import org.openmole.core.model.task.IExplorationTask;
import org.openmole.core.model.task.ITask;
import org.openmole.ui.ide.workflow.implementation.Preferences;
import org.openmole.ui.ide.workflow.implementation.TaskModelUI;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class CoreClassInstanciator {

    public static IGenericCapsule instanciateCapsule(ICapsuleModelUI capsuleModelUI) throws UserBadDataError {
        if (capsuleModelUI.getTaskModel() != TaskModelUI.EMPTY_TASK_MODEL) {
            if (Preferences.getInstance().getCoreClass(capsuleModelUI.getTaskModel().getClass()) != null) {
                if (Preferences.getInstance().getCoreClass(capsuleModelUI.getTaskModel().getClass()).equals(org.openmole.core.implementation.task.ExplorationTask.class)) {
                    try {
                        return new ExplorationCapsule((IExplorationTask) Instanciator.instanciate(Preferences.getInstance().getCoreClass(capsuleModelUI.getTaskModel().getClass()),
                                capsuleModelUI.getTaskModel().getName()));
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
                } else {
                    try {
                        return new Capsule((ITask) Instanciator.instanciate(Preferences.getInstance().getCoreClass(capsuleModelUI.getTaskModel().getClass()),
                                capsuleModelUI.getTaskModel().getName()));
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

            } else {
                throw new UserBadDataError("The task " + capsuleModelUI.getTaskModel().getClass().getCanonicalName() + " has no implementation");
            }
        } else {
            return new Capsule();
        }

    }
}
