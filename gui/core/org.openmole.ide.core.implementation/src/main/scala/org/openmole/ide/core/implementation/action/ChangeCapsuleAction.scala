/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.action

import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.model.commons.CapsuleType
import org.openmole.ide.core.model.workflow.ICapsuleUI

import scala.swing.Action

class ChangeCapsuleAction(capsule: ICapsuleUI,
                          newType: CapsuleType) extends Action(newType.toString.toLowerCase) {
  override def apply = {
    capsule -- newType
    CheckData.checkMole(capsule.scene)
    capsule.scene.manager.invalidateCache
    capsule.scene.graphScene.revalidate
    capsule.scene.graphScene.repaint
  }
}
