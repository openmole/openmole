/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.hook.file

import java.io.File

import org.openmole.core.argument.FromContext
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.tool.stream._

object AppendToFileHook {

  def apply(file: FromContext[File], content: FromContext[String])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Hook("AppendToFileHook") { p =>
      import p._
      val f = file.from(context)
      f.createParentDirectory
      // FIXME lock may not be necessary anymore - see you in 2020
      f.withLock(_.append(content.from(context)))
      context
    } withValidate { file.validate ++ content.validate }

}

