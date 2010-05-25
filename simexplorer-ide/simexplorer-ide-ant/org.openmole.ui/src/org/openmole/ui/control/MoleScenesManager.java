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

package org.openmole.ui.control;

import java.util.ArrayList;
import java.util.Collection;
import org.openmole.ui.workflow.model.IMoleScene;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MoleScenesManager {

    private static MoleScenesManager instance = null;
    
    private Collection<IMoleScene> moleScenes = new ArrayList<IMoleScene>();

    public void addMoleScene(IMoleScene ms){
        moleScenes.add(ms);
    }

    Collection<IMoleScene> getMoleScenes(){
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
}
