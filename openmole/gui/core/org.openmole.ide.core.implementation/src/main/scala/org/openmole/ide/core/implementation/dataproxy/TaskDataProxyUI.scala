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

package org.openmole.ide.core.implementation.dataproxy

import org.openmole.ide.core.implementation.data.{ ImageView, TaskDataUI }
import org.openmole.ide.core.implementation.panel.IOFacade
import org.openmole.ide.core.implementation.serializer.Update
import org.openmole.ide.misc.tools.util.ID

object TaskDataProxyUI {
  def apply(d: TaskDataUI with ImageView,
            g: Boolean = false) = new TaskDataProxyUI(d, g)

  @deprecated("Used for deserialiation purposes")
  private def annonymous = new TaskDataProxyUI(???, ???) with Update[TaskDataProxyUI] {
    def update = new TaskDataProxyUI(dataUI, generated, id)
  }
}

class TaskDataProxyUI(var dataUI: TaskDataUI with ImageView,
                      val generated: Boolean,
                      override val id: ID.Type = ID.newId) extends DataProxyUI with IOFacade {
  type DATAUI = TaskDataUI with ImageView
}