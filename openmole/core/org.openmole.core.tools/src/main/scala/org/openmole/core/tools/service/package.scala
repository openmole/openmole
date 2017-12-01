/*
 * Copyright (C) 2012 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.tools

import java.util.UUID

import org.openmole.tool.network.LocalHostName
import org.openmole.tool.random.Random

package object service {

  @transient lazy val localRuntimeInfo =
    RuntimeInfo(LocalHostName.localHostName.getOrElse("fake:" + UUID.randomUUID().toString))

  case class RuntimeInfo(hostName: String)

  def newRNG(seed: Long) = Random(seed)

}
