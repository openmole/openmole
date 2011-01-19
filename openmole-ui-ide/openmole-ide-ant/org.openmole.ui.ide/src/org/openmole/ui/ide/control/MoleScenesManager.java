/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
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
package org.openmole.ui.ide.control;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.model.ICapsuleView;
import org.openmole.ui.ide.workflow.model.IMoleScene;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MoleScenesManager extends TabManager {

    private static MoleScenesManager instance = null;
    private Collection<IMoleScene> moleScenes = new ArrayList<IMoleScene>();
    private Map<IMoleScene, Collection<Component>> childTabs = new HashMap<IMoleScene, Collection<Component>>();
    private int count = 1;
    private int nodeCounter = 0;
    private boolean detailedView = false;

    public void removeMoleScenes() {
        moleScenes.clear();
        removeAllTabs();
    }

    public void removeMoleScene(IMoleScene molescene) {
        moleScenes.remove(molescene);
        removeTab(molescene);
    }

    public void incrementNodeName() {
        nodeCounter++;
    }

    public String getNodeName() {
        return "task" + nodeCounter;
    }

    public void addMoleScene(IMoleScene ms) {
        moleScenes.add(ms);
        childTabs.put(ms, new ArrayList<Component>());
    }

    public IMoleScene addMoleScene(){
        IMoleScene sc = new MoleScene();
        addMoleScene(sc);
        return sc;
    }

    public void addChild(IMoleScene sc,
            Component co) {
        childTabs.get(sc).add(co);
    }

    public Collection<IMoleScene> getMoleScenes() {
        return moleScenes;
    }

    public void removeCurrentSceneAndChilds(IMoleScene curs) {
        for (Component co : MoleScenesManager.getInstance().childTabs.get(curs)) {
            TaskSettingsManager.getInstance().removeTab(co);
        }
        removeMoleScene(curs);
    }

    @Override
    public void addTab(Object displayed) {
        MoleScene scene = (MoleScene) displayed;

        JComponent myView = scene.createView();
        JScrollPane moleSceneScrollPane = new JScrollPane();
        moleSceneScrollPane.setViewportView(myView);

        String name;
        if (scene.getManager().getName().equals("")) {
            name = "Mole" + count;
            count++;
        } else {
            name = scene.getManager().getName();
        }
        addMapping(displayed, moleSceneScrollPane, name);
        scene.getManager().setName(name);
    }

     public void setDetailedView(boolean detailedView) {
        this.detailedView = detailedView;
    }

    public boolean isDetailedView() {
        return detailedView;
    }

    public static MoleScenesManager getInstance() {
        if (instance == null) {
            instance = new MoleScenesManager();
        }
        return instance;
    }
}
