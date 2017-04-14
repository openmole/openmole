/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.plugin.environment.gridscale

import java.io.File

import org.openmole.core.communication.storage._
import org.openmole.tool.file._

object LogicalLinkStorage {
  def apply(root: String) = {
    val _root = root
    new LogicalLinkStorage {
      val root = _root
    }
  }
}

trait LogicalLinkStorage extends LocalStorage {

  override protected def _upload(src: File, dest: String, options: TransferOptions): Unit = {
    if (options.canMove) _mv(src.getPath, dest)
    else if (options.forceCopy) super._upload(src, dest, options)
    else new File(dest).createLinkTo(src)
  }

  override protected def _download(src: String, dest: File, options: TransferOptions): Unit = {
    if (options.canMove) _mv(src, dest.getPath)
    else if (options.forceCopy) super._download(src, dest, options)
    else dest.createLinkTo(src)
  }

}
