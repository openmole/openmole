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

import java.io.File;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.capsule.Capsule;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.implementation.mole.Mole;
import org.openmole.core.implementation.transition.Transition;
import org.openmole.core.model.mole.IMole;
import org.openmole.plugin.task.filemanagement.AppendFileTask;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.implementation.MoleSceneManager;
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;
import org.openmole.ui.ide.workflow.model.ICapsuleView;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class MoleMaker {

    public static IMole process (MoleScene scene) throws UserBadDataError{
        MoleSceneManager manager = scene.getManager();

        ICapsuleModelUI start = manager.getStartingCapsule();
        if (start != null){
            Capsule startingCapsule = new Capsule();
            Mole mole = new Mole(startingCapsule);
        return mole;
        }
        else {
            throw new UserBadDataError("A starting capsule is expected");
        }

       // for (ICapsuleView cav : manager.getCapsuleViews()){
            //capsule = new Capsule
           // System.out.println("NAME "+cav.getTaskCapsuleModel().);
      //  }

//        Capsule capsule2 = new Capsule();
//        new Transition(capsule1, capsule2);
    }
}
