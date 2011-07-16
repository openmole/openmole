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

package org.openmole.core.batch.file



import org.openmole.misc.tools.service.Logger

object URIFileCleaner extends Logger 

class URIFileCleaner(toClean: IURIFile, recursive: Boolean = false, timeOut: Boolean = true) extends Runnable {

  import URIFileCleaner._
  
  override def run = {
    try {
      if (toClean != null) {
        toClean.remove(timeOut, recursive)
      }
    } catch {
      case e => logger.log(FINE, "Cannot delete file " + toClean.location, e)
    }
  }
}
