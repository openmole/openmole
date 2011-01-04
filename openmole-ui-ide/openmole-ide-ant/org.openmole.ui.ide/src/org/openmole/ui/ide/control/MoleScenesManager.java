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

import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.model.IMoleScene;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MoleScenesManager extends TabManager{

    private static MoleScenesManager instance = null;
    
    private Collection<IMoleScene> moleScenes = new ArrayList<IMoleScene>();
    private int count=1;
    private int nodeCounter = 0;

    public void incrementNodeName() {
        nodeCounter++;
    }

    public String getNodeName() {
        return "task" + nodeCounter;
    }

    public void addMoleScene(IMoleScene ms){
        moleScenes.add(ms);
    }

    public Collection<IMoleScene> getMoleScenes(){
        return moleScenes;
    }

    public void setScenesMovable(boolean movable){
        for(IMoleScene ms:moleScenes){
            ms.setMovable(movable);
        }
    }

    public static MoleScenesManager getInstance() {
        if (instance == null) {
            instance = new MoleScenesManager();
        }
        return instance;
    }

    @Override
    public void addTab(Object displayed) {
        MoleScene scene = (MoleScene) displayed;

        JComponent myView = scene.createView();
        JScrollPane moleSceneScrollPane = new JScrollPane();
        moleSceneScrollPane.setViewportView(myView);

        addMapping(displayed, moleSceneScrollPane,"Mole"+count);
        count ++;
    }
}
