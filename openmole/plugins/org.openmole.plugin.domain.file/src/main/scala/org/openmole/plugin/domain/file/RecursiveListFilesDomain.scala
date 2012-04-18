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

package org.openmole.plugin.domain.file

import java.io.File
import java.io.FileFilter
import org.openmole.core.model.data.IContext
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IFinite
import org.openmole.misc.tools.io.FileUtil._

sealed class RecursiveListFilesDomain(dir: File, filter: FileFilter) extends IDomain[File] with IFinite[File] {

  def this(dir: File, pattern: String, shouldBeAFile: Boolean) = {
    this(dir, new FileFilter {       
        override def accept(file: File) = file.getName.matches(pattern) && (if(shouldBeAFile) file.isFile else true)
      })
  }
    
  override def computeValues(context: IContext) = dir.listRecursive(filter)
  
}
