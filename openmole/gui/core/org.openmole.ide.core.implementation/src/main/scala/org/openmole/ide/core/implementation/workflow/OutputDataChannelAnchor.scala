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

package org.openmole.ide.core.implementation.workflow

import java.awt.Point
import org.netbeans.api.visual.anchor.Anchor
import org.netbeans.api.visual.anchor.Anchor.Direction._
import org.openmole.ide.core.model.commons.Constants
import org.openmole.ide.core.model.workflow.ICapsuleUI

class OutputDataChannelAnchor(relatedWidget: ICapsuleUI) extends Anchor(relatedWidget.widget) {

  override def compute(entry: Anchor.Entry) =
    new Result(relatedWidget.widget.convertLocalToScene(new Point(Constants.TASK_CONTAINER_WIDTH + 10,
      Constants.TASK_CONTAINER_HEIGHT - 15)), Anchor.Direction.RIGHT)
}