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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.domain.file

import java.io.File
import java.io.FileFilter
import org.openmole.core.model.data.IContext
import org.openmole.core.model.domain.IFiniteDomain
import scala.collection.JavaConversions._

class ListFilesAndNamesDomain(dir: File, filter: Option[FileFilter]) extends IFiniteDomain[(File,String)] {

  @transient lazy val listFiles = new ListFilesDomain(dir, filter)
  
  def this(dir: File) = this(dir, None)

  def this(dir: File, pattern: String) = {
    this(dir, Some(new FileFilter {

          override def accept(file: File): Boolean = file.getName().matches(pattern)
            
        }))
  }

  override def computeValues(global: IContext, context: IContext): Iterable[(File, String)] = {
    listFiles.computeValues(global,context).map(f => (f,f.getName))
  }
}
