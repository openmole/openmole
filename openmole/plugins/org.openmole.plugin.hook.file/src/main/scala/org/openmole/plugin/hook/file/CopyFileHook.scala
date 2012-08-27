/*
 *  Copyright (C) 2010 Romain Reuillon <romain.Romain Reuillon at openmole.org>
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

package org.openmole.plugin.hook.file

import java.io.File

import org.openmole.core.model.mole._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.hook._
import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.implementation.tools._
import org.openmole.misc.exception.UserBadDataError

class CopyFileHook(
    moleExecution: IMoleExecution,
    capsule: ICapsule,
    filePrototype: IPrototype[File],
    destination: String,
    remove: Boolean = false,
    compress: Boolean = false) extends CapsuleExecutionHook(moleExecution, capsule) {

  override def process(moleJob: IMoleJob) = {
    import moleJob.context

    context.value(filePrototype) match {
      case Some(from) ⇒
        val to = new File(VariableExpansion(context, destination))

        to.getParentFile.mkdirs
        if (compress) from.copyCompress(to)
        else from.copy(to)

        if (remove) from.recursiveDelete
      case None ⇒ throw new UserBadDataError("No variable " + filePrototype + " found.")
    }
  }

  def inputs = DataSet(filePrototype)

}
