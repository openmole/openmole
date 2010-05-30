/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.external

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import java.io.File
import org.openmole.commons.tools.io.FastCopy
import org.openmole.commons.tools.io.IFileOperation

abstract class ExternalSystemTask(name: String) extends ExternalTask(name) {

  protected def copyTo(from: File, to: File) = {
    FastCopy.copy(from, to)

    FastCopy.applyRecursive(to, new IFileOperation() {

        override def execute(file: File) =  {
          if (file.isFile()) {
            file.setExecutable(true)
          }
          file.deleteOnExit
        }
      })
  }

  protected def copyFrom(from: File): File = {
     if (!from.exists()) {
          throw new UserBadDataError("Output file " + from.getAbsolutePath + " for task " + getName + " doesn't exist")
     }
     return from
  }

}
