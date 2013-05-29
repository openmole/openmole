/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.ui.console

import java.io.File
import org.openmole.core.model.task.PluginSet
import org.openmole.core.model.mole.ExecutionContext
import org.openmole.core.model.data.Context

class Implicits(
    implicit val plugins: PluginSet) {
  implicit def stringToFile(path: String) = new File(path)
  implicit lazy val executionContext = ExecutionContext.local
  implicit lazy val implicits = Context.empty
}

