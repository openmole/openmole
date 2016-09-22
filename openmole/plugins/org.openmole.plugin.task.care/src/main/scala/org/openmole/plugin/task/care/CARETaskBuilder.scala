/*
 *  Copyright (C) 2015 Jonathan Passerat-Palmbach
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.care

import monocle.Lens
import org.openmole.plugin.task.systemexec._

trait CARETaskBuilder[T] extends ReturnValue[T]
    with ErrorOnReturnValue[T]
    with StdOutErr[T]
    with EnvironmentVariables[T]
    with WorkDirectory[T] { builder ⇒

  def hostFiles: Lens[T, Vector[(String, Option[String])]]

}
