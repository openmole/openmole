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
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IFinite
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.service.Logger

object SlidingSliceFilesDomain extends Logger {
  val defaultPattern = "^[a-zA-Z]*([0-9]+).*"
}

import SlidingSliceFilesDomain._

sealed class SlidingSliceFilesDomain(dir: File, numberPattern: String, sliceSize: Int, filter: File ⇒ Boolean) extends IDomain[Array[File]] with IFinite[Array[File]] {

  def this(dir: File, numberPattern: String, sliceSize: Int) = this(dir, numberPattern, sliceSize, f ⇒ true)

  def this(dir: File, sliceSize: Int) = this(dir, SlidingSliceFilesDomain.defaultPattern, sliceSize)

  def this(dir: File, sliceSize: Int, filter: File ⇒ Boolean) = this(dir, SlidingSliceFilesDomain.defaultPattern, sliceSize, filter)

  override def computeValues(context: IContext): Iterable[Array[File]] = {
    val pattern = Pattern.compile(numberPattern)

    val files = dir.listFiles(filter).flatMap {
      f ⇒
        val m = pattern.matcher(f.getName)
        if (m.find) Some(f, m.group(1).toLong)
        else {
          logger.warning("File " + f.getName + " in dir " + dir + " doesn't match regexp " + numberPattern + ", it has been ignored.")
          None
        }
    }.sortBy { case (f, num) ⇒ num }.map { case (f, num) ⇒ f }

    (0 to files.size - sliceSize).map {
      i ⇒ files.slice(i, i + sliceSize)
    }
  }

}
