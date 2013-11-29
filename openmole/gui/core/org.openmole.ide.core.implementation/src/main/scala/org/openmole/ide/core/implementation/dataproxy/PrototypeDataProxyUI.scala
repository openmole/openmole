/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.dataproxy

import org.openmole.ide.core.implementation.data.{ ImageView, PrototypeDataUI }
import org.openmole.ide.core.implementation.serializer.Update
import org.openmole.ide.misc.tools.util.ID

object PrototypeDataProxyUI {
  def apply(d: PrototypeDataUI[_] with ImageView,
            g: Boolean = false) = new PrototypeDataProxyUI(d, g)

  @deprecated("Used for deserialiation purposes")
  private def annonymous = new PrototypeDataProxyUI(???, ???) with Update[PrototypeDataProxyUI] {
    def update = new PrototypeDataProxyUI(dataUI, generated, id)
  }
}

class PrototypeDataProxyUI(var dataUI: PrototypeDataUI[_] with ImageView,
                           val generated: Boolean,
                           override val id: ID.Type = ID.newId) extends DataProxyUI {
  type DATAUI = PrototypeDataUI[_] with ImageView

  override def toString = {
    if (dataUI.dim > 0) dataUI.name + " [" + dataUI.dim + "]"
    else dataUI.name
  }
}

