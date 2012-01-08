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

package org.openmole.core.batch.message

object FileMessage {
  val EMPTY_RESULT = new FileMessage(null, null)
  
  implicit def replicatedFile2FileMessage(r: ReplicatedFile) = new FileMessage(r)
}

class FileMessage(val path: String, val hash: String) {
  def this(replicatedFile: ReplicatedFile) = this(replicatedFile.replicaPath, replicatedFile.hash)
  
  def isEmpty: Boolean = path == null
}
