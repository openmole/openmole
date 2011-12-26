/*
 * Copyright (C) 2011 leclaire
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.control

import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.model.control.IMoleComponent
import scala.collection.mutable.HashSet

class BuildMoleComponent(val moleScene: BuildMoleScene) extends IMoleComponent{
  override val moleSceneTopComponent = new MoleSceneTopComponent
  moleSceneTopComponent.initialize(moleScene,this)
  var executionMoleSceneComponents = new HashSet[ExecutionMoleComponent]
  
  def stopAndCloseExecutions = {
    executionMoleSceneComponents.foreach{emc=>
      emc.executionManager.cancel
        moleSceneTopComponent.close
    }
    executionMoleSceneComponents.clear
  }
}
