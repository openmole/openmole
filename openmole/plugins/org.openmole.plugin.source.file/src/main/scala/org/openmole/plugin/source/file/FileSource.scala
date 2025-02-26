/*
 * Copyright (C) 09/07/13 Romain Reuillon
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

package org.openmole.plugin.source.file

import java.io.File
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object FileSource {

  def apply(path: FromContext[String], prototype: Val[File])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Source("FileSource") { p =>
      import p._
      val expandedPath = new File(path.from(context))
      Variable(prototype, expandedPath)
    } set (outputs += prototype)

}

