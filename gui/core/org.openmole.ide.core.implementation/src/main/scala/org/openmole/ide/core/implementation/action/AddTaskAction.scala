/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.action

import org.openmole.ide.core.implementation.provider.GenericMenuProvider
import org.openmole.ide.core.implementation.workflow.SceneItemFactory
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.swing.Action

class AddTaskAction(moleScene: IMoleScene,
                    dpu: ITaskDataProxyUI,
                    provider: GenericMenuProvider) extends Action(dpu.dataUI.name) {
  override def apply = {
    val capsule = SceneItemFactory.createCapsule(moleScene, provider.currentPoint, Some(dpu))
    capsule.addInputSlot(moleScene.manager.capsules.size == 1)
  }
}