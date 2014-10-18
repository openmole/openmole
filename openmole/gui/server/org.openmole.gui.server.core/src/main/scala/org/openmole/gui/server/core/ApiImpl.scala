package org.openmole.gui.server.core

import org.openmole.gui.server.factory.ServerFactories
import org.openmole.gui.shared.Api

/*
 * Copyright (C) 10/10/14 // mathieu.leclaire@openmole.org
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

object ApiImpl extends Api {
  def factoriesUI: Map[String, String] = ServerFactories.uiFactories.toMap
  //def factoriesUI: Seq[(Class[_], String)] = ServerFactories.uiFactories.toSeq
}
