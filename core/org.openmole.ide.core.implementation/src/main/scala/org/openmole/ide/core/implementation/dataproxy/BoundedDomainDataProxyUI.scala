/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.dataproxy

import org.openmole.ide.core.model.data.IBoundedDomainDataUI
import org.openmole.ide.core.model.dataproxy.IBoundedDomainDataProxyUI
import org.openmole.ide.misc.tools.Counter

class BoundedDomainDataProxyUI(name: String,
                               var dataUI: IBoundedDomainDataUI,
                               val id: Int = Counter.id.getAndIncrement) extends IBoundedDomainDataProxyUI {
  override def toString = name
}