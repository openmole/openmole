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

package org.openmole.ide.core.implementation.workflow

import java.awt.Point
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.tools.image.Images._

class OutputSlotWidget(scene: IMoleScene, val capsule: ICapsuleUI) extends SlotWidget(scene.graphScene) {
  setPreferredLocation(new Point(TASK_CONTAINER_WIDTH, 24 + TASK_TITLE_HEIGHT))

  scene match {
    case x: ExecutionMoleScene ⇒ setImage(OUTPUT_EXE_SLOT)
    case _ ⇒ setImage(OUTPUT_SLOT)
  }
}
