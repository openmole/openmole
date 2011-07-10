/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.plugin.task.code

import org.openmole.core.model.data.IContext
import org.openmole.core.implementation.data.Context._
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.task.external.system.ExternalSystemTask
import org.openmole.plugin.tools.code.{IContextToCode,ISourceCode}

abstract class CodeTask(name: String) extends ExternalSystemTask(name) {

  override def process(context: IContext) = {
    val pwd = Workspace.newDir
    prepareInputFiles(context, pwd.getCanonicalFile)
    fetchOutputFiles(contextToCode.execute(context, userOutputs).toContext, pwd.getCanonicalFile)
  }

  def contextToCode: IContextToCode

}
