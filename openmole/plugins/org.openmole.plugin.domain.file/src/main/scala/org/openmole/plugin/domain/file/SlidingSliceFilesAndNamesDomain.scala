/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.domain.file

import java.io.File
import java.util.regex.Pattern
import org.openmole.core.model.data.IContext
import org.openmole.core.model.domain.IFiniteDomain
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FileUtil._

object SlidingSliceFilesAndNamesDomain {
  val defaultPattern = "^[a-zA-Z]*([0-9]+).*"
}

class SlidingSliceFilesAndNamesDomain(dir: File, numberPattern: String, sliceSize: Int, filter: File => Boolean) extends IFiniteDomain[Array[(File,String)]] {
 
  def this(dir: File, numberPattern: String, sliceSize: Int) = this(dir, numberPattern, sliceSize, f => true)
  
  def this(dir: File, sliceSize: Int) = this(dir, SlidingSliceFilesAndNamesDomain.defaultPattern, sliceSize)
  
  def this(dir: File, sliceSize: Int, filter: File => Boolean) = this(dir, SlidingSliceFilesAndNamesDomain.defaultPattern, sliceSize, filter)

  override def computeValues(context: IContext): Iterable[Array[(File,String)]] = {
    val pattern = Pattern.compile(numberPattern)
    
    val files = dir.listFiles(filter).sortBy(f => pattern.matcher(f.getName).group(1) match {
        case null => throw new UserBadDataError("File " + f + " doesn't match regexp " + numberPattern)
        case s: String => s.toLong
      })
    
    (0 until files.size - sliceSize).map{
      i => files.slice(i, i + sliceSize).map(f => f -> f.getName)
    }
  }

}
