/*
 *  Copyright (C) 2011 Mathieu Leclaire <mathieu.leclaire@openmole.org>
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
package org.openmole.ui.ide.control;

import java.awt.Component;
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.model.ICapsuleView;
import org.openmole.ui.ide.workflow.model.IMoleScene;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class TabsManager {

    private static TabsManager instance = null;

    private Object getCurrentObject() {
        Object ob = MoleScenesManager.getInstance().getCurrentObject();
        if (ob == null) {
            return TaskSettingsManager.getInstance().getCurrentObject();
        }
        return ob;
    }

    private IMoleScene getCurrentScene() {
        Object o = getCurrentObject();
        if (o.getClass().equals(CapsuleViewUI.class)) {
            return ((ICapsuleView) o).getMoleScene();
        }
        return (MoleScene) o;
    }

    public void removeCurrentSceneAndChild() {
        MoleScenesManager.getInstance().removeCurrentSceneAndChilds(getCurrentScene());
    }

    public static TabsManager getInstance() {
        if (instance == null) {
            instance = new TabsManager();
        }
        return instance;
    }
}
