/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
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

package org.openmole.ui.workflow.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import org.openide.util.Exceptions;
import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.commons.tools.object.Instanciator;
import org.openmole.ui.workflow.model.IMoleScene;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class AddTaskAction implements ActionListener {

    IMoleScene moleScene;
    Class<? extends IGenericTask> taskClass;

    public AddTaskAction(IMoleScene moleScene,
                         Class<? extends IGenericTask> taskClass) {
        this.moleScene = moleScene;
        this.taskClass = taskClass;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        try {
            moleScene.createTask(Instanciator.instanciate(taskClass));
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
        moleScene.refresh();
    }
}